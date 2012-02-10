package com.nbos.phonebook.sync.platform;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.util.Log;

import com.nbos.phonebook.Db;
import com.nbos.phonebook.sync.Constants;
import com.nbos.phonebook.sync.client.Contact;
import com.nbos.phonebook.sync.client.ContactPicture;
import com.nbos.phonebook.sync.client.Net;
import com.nbos.phonebook.sync.client.ServerData;
import com.nbos.phonebook.util.ImageInfo;
import com.nbos.phonebook.value.PicData;

public class SyncPics {
	static String tag = "SyncPics";
	Context context;
	ContentResolver cr;
	Db db;
	Map<String, PicData> serverPicData;
    Set<String> unchangedPicsRawContactIds  = new HashSet<String>(), 
    	syncedPictureIds = new HashSet<String>();

    Map<String, String> serverPicIds = new HashMap<String, String>();
    Map<String, Integer> contactPicturesIndex;
    BatchOperation updatePicBatchOperation,	updatePicIdBatchOperation;
	Cursor dataPicsCursor;
	SyncManager syncManager;
	Cloud cloud;
    int batchSize = 50;

    SyncPics(Context context, Map<String, PicData> map, Cloud cloud) {
    	this.context = context;
    	this.serverPicData = map;
    	this.cloud = cloud;
    	db = new Db(context);
    	cr = context.getContentResolver();
		getCursors();
		getContactPicturesIndex();
    }

	public void sync(List<Contact> contacts, SyncManager syncManager) {
		this.syncManager = syncManager;
		updatePicBatchOperation = new BatchOperation(context);
    	updatePicIdBatchOperation = new BatchOperation(context);
		for(Contact c : contacts) {
			if(c.picId == null) continue;
			syncPicture(c);
			if(updatePicBatchOperation.size() > batchSize)
			{
				Log.i(tag, "Executing update pic batch operation: "+updatePicBatchOperation.size());
				updatePicBatchOperation.execute();
			}
			
			if(updatePicIdBatchOperation.size() > batchSize)
			{
				Log.i(tag, "Executing update pic id batch operation: "+updatePicIdBatchOperation.size());
				updatePicIdBatchOperation.execute();
			}
		}
		
		Log.i(tag, "Executing last update pic batch operation: "+updatePicBatchOperation.size());
		updatePicBatchOperation.execute();
		Log.i(tag, "Executing last update pic id batch operation: "+updatePicIdBatchOperation.size());
		updatePicIdBatchOperation.execute();
	}
	
	Map<String, Integer> getContactPicturesIndex() {
		contactPicturesIndex = new HashMap<String, Integer>();
		dataPicsCursor.requery();
		dataPicsCursor.moveToFirst();
		if(dataPicsCursor.getCount() == 0) 
			return contactPicturesIndex;
		do {
	    	String mimetype = dataPicsCursor.getString(dataPicsCursor.getColumnIndex(Data.MIMETYPE));
	    	if(!mimetype.equals(Photo.CONTENT_ITEM_TYPE)) continue;
	    	String name = dataPicsCursor.getString(dataPicsCursor.getColumnIndex(Data.DISPLAY_NAME));
	    	String rawId = dataPicsCursor.getString(dataPicsCursor.getColumnIndex(Data.RAW_CONTACT_ID));
	    	Log.i(tag, name+", raw: "+rawId+", index: "+dataPicsCursor.getPosition());
	    	contactPicturesIndex.put(rawId, new Integer(dataPicsCursor.getPosition()));//new ContactPicture(pic, contentType));	    	
		} while(dataPicsCursor.moveToNext());
		return contactPicturesIndex;
	}
	
	private void syncPicture(Contact c) {
		// check if the pic is the same in the server
		Cursor serverDataCursor = cloud.serverDataCursor;
		if(serverDataCursor.getCount() > 0)
		{
			Integer index = cloud.serverDataIndex.get(c.serverId);
			if(index != null)
			{
				serverDataCursor.moveToPosition(index);
				String rawContactId = serverDataCursor.getString(serverDataCursor.getColumnIndex(Data.RAW_CONTACT_ID)),
					picId = serverDataCursor.getString(serverDataCursor.getColumnIndex(PhonebookSyncAdapterColumns.PIC_ID));
				
				if(c.picId.equals(picId)) // no change in pic 
				{
					unchangedPicsRawContactIds.add(rawContactId);
					Log.i(tag, "no change in pic: "+c.picId+", serverId: "+c.serverId);
					return;
				}
			}
		}
		updateContactPic(c);
	}

	
	private void updateContactPic(Contact c) {
		if(syncedPictureIds.contains(c.picId))
		{
			Log.d(tag, "Already synced picture id: "+c.picId);
			return;
		}

		// 
		Long rawContactId = syncManager.lookupRawContact(c);
        ContactPicture contactPicture = getContactPicture(rawContactId.toString(), false);
        // if(contactPicture == null) return;
    	byte[] phonePhoto = contactPicture == null ? null : contactPicture.pic;

    	// is server pic null mean its not in the server table
    	if(isServerPic(c.serverId, phonePhoto)) // see if pic is already in the server data
    	{
    		Log.i(tag, "Photo is same on server");
    		unchangedPicsRawContactIds.add(rawContactId.toString());
    		return;
    	}
    	
    	byte[] serverPhoto = downloadPic(c);
   		if(serverPhoto == null) return;
   		// TODO: delete the phone photo if server photo is null?
    	
        Uri uri = SyncManager.addCallerIsSyncAdapterParameter(Data.CONTENT_URI);
    	if(phonePhoto != null) // already has a photo, update
    	{
	    	Log.i(tag, "Updating pic for serverId: "+c.serverId);
	    	ContentValues values = new ContentValues();
	    	values.put(Photo.PHOTO, serverPhoto);
	    	context.getContentResolver().update(uri, values, 
	    			Data.RAW_CONTACT_ID + " = ? and " +
	    			Data.MIMETYPE + " = ? ", 
	    			new String[] {rawContactId.toString(), Photo.CONTENT_ITEM_TYPE});
    	}
    	else 
        {
	        Log.i(tag, "inserting pic for serverId: "+c.serverId+", image is: "+serverPhoto.length+", rawContactId: "+rawContactId);
			ContentProviderOperation.Builder builder = 
				ContentProviderOperation.newInsert(uri)
		            .withYieldAllowed(true);
	        
	        ContentValues values = new ContentValues();
	        values.put(Data.RAW_CONTACT_ID, rawContactId);
	        values.put(Data.IS_SUPER_PRIMARY, 1);
	        // values.put(ContactsContract.CALLER_IS_SYNCADAPTER, "true");
	        values.put(Photo.PHOTO, serverPhoto);
	        values.put(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
	        // Uri result = context.getContentResolver().insert(uri, values);
	        builder.withValues(values);
	        updatePicBatchOperation.add(builder.build());
	        
	        // Log.i(tag, "pic uri is: "+result);
        }
        
    	if(cloud.serverIds.contains(c.serverId))
    		updateContactPicData(rawContactId.toString(), c.serverId, c.picId, new Long(serverPhoto.length).toString(), ImageInfo.hash(serverPhoto));
    	else
    	{
    		insertServerData(rawContactId.toString(), c.serverId, c.picId, new Long(serverPhoto.length).toString(), ImageInfo.hash(serverPhoto));
    		serverPicIds.put(c.serverId, c.picId);
    	}
		syncedPictureIds.add(c.picId);
	}

	private byte[] downloadPic(Contact c) {
		URL url;
		try {
			url = new URL(Cloud.DOWNLOAD_CONTACT_PIC_URI + c.picId);
			URLConnection ucon = url.openConnection();
	        InputStream is = ucon.getInputStream();
	        BufferedInputStream bis = new BufferedInputStream(is, 8192);
	        /*
	         * Read bytes to the Buffer until there is nothing more to read(-1).
	         */
	        ByteArrayBuffer baf = new ByteArrayBuffer(50);
	        int current = 0;
	        while ((current = bis.read()) != -1) 
	        	baf.append((byte) current);
	        byte[] image = baf.toByteArray();
	        Log.i(tag, "Contact: "+c.serverId+", image size: "+image.length);
	        return image;
		} catch (Exception e) {
			Log.e(tag, "Exception downloading pic: "+e);
			e.printStackTrace();
		}
		return null;
	}
	
	public ContactPicture getContactPicture(String rawContactId, boolean getContentType) {
		Log.d(tag, "getContactPicture("+rawContactId+")");
		Integer index = contactPicturesIndex.get(rawContactId);
		if(index == null)
		{
			Log.d(tag, "No picture");
			return null;
		}
		dataPicsCursor.moveToPosition(index);
		byte[] pic = dataPicsCursor.getBlob(dataPicsCursor.getColumnIndex(Photo.PHOTO));
		String name = dataPicsCursor.getString(dataPicsCursor.getColumnIndex(Contacts.DISPLAY_NAME));
    	String contentType = null;
    	
    	if(getContentType)
		try {
			contentType = new ImageInfo(pic).getMimeType();
		} catch (IOException e) {
			Log.e(tag, "Exception getting pic mime type: "+e);
			e.printStackTrace();
		} 
		
		Log.i(tag, "Contact["+rawContactId+"] "+name+", pic: "+(pic == null ? "null" : pic.length+", content type: "+contentType+ " ,index:"+index));
		return new ContactPicture(pic, contentType);
	}
	
	public void updateContactPicData(String rawContactId, String serverId, String picId, String picSize, String hash) {
		Uri uri = Data.CONTENT_URI;
		ContentValues values = new ContentValues();
		values.put(PhonebookSyncAdapterColumns.PIC_ID, picId);
		values.put(PhonebookSyncAdapterColumns.PIC_SIZE, picSize);
		values.put(PhonebookSyncAdapterColumns.PIC_HASH, hash);
		int num = context.getContentResolver().update(uri, values, 
				Data.RAW_CONTACT_ID + " = " + rawContactId + " and " +
				PhonebookSyncAdapterColumns.DATA_PID + " = " + serverId + " and " +
				Data.MIMETYPE + " = '" + PhonebookSyncAdapterColumns.MIME_PROFILE + "' " +
				"and "+PhonebookSyncAdapterColumns.ACCOUNT + " = '"+cloud.account+"'"
				, null);
		Log.i(tag, "updated "+num+" rows to picId: "+picId+" for serverId:"+serverId);
	}

	void insertServerData(String rawContactId, String serverId, String picId, String picSize, String hash) 
	{
		Log.i(tag, "insertServerData - raw: "+rawContactId+", serverId: "+serverId+", picId: "+picId+", picSize: "+picSize+", hash: "+hash);
		Uri uri = SyncManager.addCallerIsSyncAdapterParameter(Data.CONTENT_URI);
		ContentProviderOperation.Builder builder = 
			ContentProviderOperation.newInsert(uri)
	            .withYieldAllowed(true);
        
		
    	ContentValues values = new ContentValues();
        values.put(PhonebookSyncAdapterColumns.DATA_PID, serverId);
        values.put(PhonebookSyncAdapterColumns.ACCOUNT, cloud.account);
        values.put(Data.RAW_CONTACT_ID, rawContactId);        
        values.put(Data.MIMETYPE, PhonebookSyncAdapterColumns.MIME_PROFILE);
		values.put(PhonebookSyncAdapterColumns.PIC_ID, picId);
		values.put(PhonebookSyncAdapterColumns.PIC_SIZE, picSize);
		values.put(PhonebookSyncAdapterColumns.PIC_HASH, hash);
        builder.withValues(values);
        updatePicIdBatchOperation.add(builder.build());
		// Uri result = context.getContentResolver().insert(uri, values);
		// Log.i(tag, "updated "+result);
	}

	private void getCursors() {
    	dataPicsCursor = cr.query(ContactsContract.Data.CONTENT_URI,
	    		// null,
	    	    new String[] {
	    			Data.CONTACT_ID,
	    			Contacts.DISPLAY_NAME,
	    			Data.RAW_CONTACT_ID,
	    			Data.MIMETYPE,
	    			Photo.PHOTO,
	    		},
	    		Photo.PHOTO +" is not null "
	    		// null,
	    		+ " and "+Data.MIMETYPE+" = ? ",
	    	    new String[] {ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE}, 
	    	    ContactsContract.Data.CONTACT_ID);
    	Log.i(tag, "Data pics cursor has "+dataPicsCursor.getCount()+" rows");
	}

	public void closeCursors() {
		dataPicsCursor.close();
	}
	
	boolean isServerPic(String serverId, byte[] photo) {
		PicData picData = serverPicData.get(serverId);
		if(picData != null && photo != null 
		&& picData.picSize == photo.length) 
			return true;
		return false;
	}

	public void send(boolean newOnly) throws ClientProtocolException, JSONException, IOException {
	    getContactPicturesIndex();
	    cloud.getServerDataIds();
	    Log.i(tag, "There are "+contactPicturesIndex.size()+" contact pictures");
	    if(contactPicturesIndex.size() == 0)
	    	return;
		
	    Map<String, String> params = getUploadParams(); //new HashMap<String, String>();
		Cursor rawContactsCursor = db.getRawContactsCursor(newOnly);
		Log.i(tag, "There are "+rawContactsCursor.getCount()+" raw contacts entries");
		if(rawContactsCursor.getCount() == 0) return;
	    // Cursor dataCursor = db.getProfileData();
	    rawContactsCursor.moveToFirst();
	    do {
	    	String rawContactId = rawContactsCursor.getString(rawContactsCursor.getColumnIndex(ContactsContract.RawContacts._ID));
	    	if(unchangedPicsRawContactIds.contains(rawContactId))
	    	{
	    		Log.i(tag, "Unchanged pic");
	    		continue;
	    	}
	    	
	    	ServerData data = getServerDataFromContactId(rawContactId);
	    	Log.i(tag, "Server data: "+data);
	    	if(data == null)
	    	{
	    		Log.i(tag, "No server data");
	    		continue;
	    	}
	    	
	    	if(data.picId != null && syncedPictureIds.contains(data.picId))
	    	{
	    		Log.i(tag, "Already synced picure");
	    		continue;
	    	}

	    	ContactPicture pic = null;
			try {
				pic = getContactPicture(rawContactId, true);//getContactPicture(photosDataCursor, rawContactId);
				if(pic == null) 
				{
					Log.i(tag, "No pic");
					continue;
				}
				String hash = ImageInfo.hash(pic.pic);
		        Log.i(tag, "hash: " + hash);
		        
		        String serverPicId = serverPicIds.get(data.serverId);
		        Log.i(tag, "serverPicId: "+serverPicId);
		        if(serverPicId != null)
		        {
		        	Log.i(tag, "updating pic id from sync serverPicId");
		        	updateContactPicData(rawContactId, data.serverId, serverPicId, new Long(pic.pic.length).toString(), hash);
		        	continue;
		        }

				if(data.picId != null && data.picSize != null) 
				{
					int pSize = Integer.parseInt(data.picSize);
					if(pSize == pic.pic.length 
					&& data.picHash != null && hash.equals(data.picHash)
					&& isServerPicData(data))
					{
						Log.i(tag, "Same image not uploading");
						continue;
					}
				}
	    		String contentType = pic.mimeType.split("/")[1];
	    		Log.i(tag, "uploading "+contentType);
	    		params.remove("id");
	    		params.put("id", data.serverId);
	    		JSONObject response = upload(Cloud.UPLOAD_CONTACT_PIC_URI, pic.pic, contentType, params);
	    		if(response != null)
	    		{
	    			try {
						int status = response.getInt("ok");
						if(status == 1)
						{
							String pId = response.getString("id");
								// pSize = response.getString("size"),
								// hash = response.getString("hash");
							updateContactPicData(rawContactId, data.serverId, pId, new Long(pic.pic.length).toString(), hash);
						}
					} catch (JSONException e) {
						Log.e(tag, "JSON Exception: "+e);
						e.printStackTrace();
					}
	    		}
			} catch (Exception e) {
				Log.e(tag, "Error getting picture: "+e);
				e.printStackTrace();
			}
	    } while(rawContactsCursor.moveToNext());
	    rawContactsCursor.close();
	}

	private Map<String, String> getUploadParams() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("upload", "avatar");
		params.put("errorAction", "error");
		params.put("errorController", "file");
		params.put("successAction", "success");
		params.put("successController", "file");
		return params;
	}

	private boolean isServerPicData(ServerData data) {
		PicData p = serverPicData.get(data.serverId);
		if(p != null && p.picId != null
		&& p.picId.equals(data.picId))
			return true;
		return false;
	}

	JSONObject upload(String uploadUrl, byte[] data, String contentType, Map<String, String> params) {
		// Log.i(tag, "Uploading to "+uploadUrl);
		HttpURLConnection connection = null;
		DataOutputStream outputStream = null;

		// String pathToOurFile = "/tmp/avatar/1310118336631/card_errors.gif";
		// String uploadUrl = "http://10.9.8.29:8080/phonebook/fileUploader/process";
		String lineEnd = "\r\n";
		String twoHyphens = "--";
		String boundary =  "*****";

		try
		{
		URL url = new URL(uploadUrl);
		connection = (HttpURLConnection) url.openConnection();

		// Allow Inputs & Outputs
		// connection.setDoInput(true);
		connection.setDoOutput(true);
		// connection.setUseCaches(false);

		// Enable POST method
		connection.setRequestMethod("POST");

		connection.setRequestProperty("Connection", "Keep-Alive");
		connection.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);
		
		//connection.setInstanceFollowRedirects(true);
		// HttpURLConnection.setFollowRedirects(true);
		// Log.i(tag, "Follow redirects: "+connection.getInstanceFollowRedirects());

		outputStream = new DataOutputStream( connection.getOutputStream() );
		for (Map.Entry<String, String> entry : params.entrySet()) {
		    String name = entry.getKey();
		    String value = entry.getValue();
		    // Log.i(tag, "param: "+name+", value: "+value);
		    if(value == null) continue;
		    outputStream.writeBytes(twoHyphens + boundary + lineEnd);
		    outputStream.writeBytes("Content-Disposition: form-data; name=\""+name+"\"" + lineEnd);
		    outputStream.writeBytes(lineEnd);
		    outputStream.writeBytes(value);
		    outputStream.writeBytes(lineEnd);
		}

		outputStream.writeBytes(twoHyphens + boundary + lineEnd);
		// outputStream.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\"" + pathToOurFile +"\"" + lineEnd);
		outputStream.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\"" + "image."+contentType +"\"" + lineEnd);
		outputStream.writeBytes(lineEnd);

		outputStream.write(data, 0, data.length);

		outputStream.writeBytes(lineEnd);
		outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

		// Responses from the server (code and message)
		int serverResponseCode = connection.getResponseCode();
		String location = connection.getHeaderField("Location");
		// Log.i(tag, "response code: "+serverResponseCode+", message: "+serverResponseMessage+", location: "+location);
		if(serverResponseCode == 302)
		{
			url = new URL(location);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.connect();
		}
		
		InputStream in = new BufferedInputStream(connection.getInputStream(), 8*1024);
		String response = convertStreamToString(in);
		Log.i(tag, "Response: "+response);
		outputStream.flush();
		outputStream.close();
		return new JSONObject(response);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}		
		return null;
	}
	
    String convertStreamToString(InputStream is) throws IOException {
	/*
	 * To convert the InputStream to String we use the
	 * Reader.read(char[] buffer) method. We iterate until the
	 * Reader return -1 which means there's no more data to
	 * read. We use the StringWriter class to produce the string.
	 */
		if (is != null) {
		    Writer writer = new StringWriter();
		
		    char[] buffer = new char[1024];
		    try {
		        Reader reader = new BufferedReader(
		                new InputStreamReader(is, "UTF-8"));
		        int n;
		        while ((n = reader.read(buffer)) != -1) {
		            writer.write(buffer, 0, n);
		        }
		    } finally {
		        is.close();
		    }
		    return writer.toString();
		} else {        
		    return "";
		}
    }

	public ServerData getServerDataFromContactId(String rawContactId) {
		Log.i(tag, "server data for raw contact id: "+rawContactId);
		String serverId = cloud.contactServerIdsMap.get(rawContactId);
		if(serverId == null) return null;
		Integer index = cloud.serverDataIndex.get(serverId);
		Cursor serverDataCursor = cloud.serverDataCursor;
		serverDataCursor.moveToPosition(index);
		String rawId = serverDataCursor.getString(serverDataCursor.getColumnIndex(Data.RAW_CONTACT_ID)),
			sId = serverDataCursor.getString(serverDataCursor.getColumnIndex(PhonebookSyncAdapterColumns.DATA_PID)),
			picId = serverDataCursor.getString(serverDataCursor.getColumnIndex(PhonebookSyncAdapterColumns.PIC_ID)),
			picSize = serverDataCursor.getString(serverDataCursor.getColumnIndex(PhonebookSyncAdapterColumns.PIC_SIZE)),
			picHash = serverDataCursor.getString(serverDataCursor.getColumnIndex(PhonebookSyncAdapterColumns.PIC_HASH));
		return new ServerData(rawId, sId, picId, picSize, picHash);
	}
}
