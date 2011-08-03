package com.nbos.phonebook.sync.platform;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;

import android.accounts.Account;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.nbos.phonebook.Db;
import com.nbos.phonebook.sync.client.Contact;
import com.nbos.phonebook.sync.client.ContactPicture;
import com.nbos.phonebook.sync.client.Group;
import com.nbos.phonebook.sync.client.Net;
import com.nbos.phonebook.sync.client.PhoneContact;
import com.nbos.phonebook.sync.client.SharingBook;

public class Cloud {
	static String tag = "Cloud";
	
    Context context;
    String accountName, authToken;
    HttpClient httpClient;
    public static final int REGISTRATION_TIMEOUT = 30 * 1000; // ms

    public static final String 
    	PARAM_USERNAME = "username",
    	PARAM_PASSWORD = "password",
    	PARAM_PHONE_NUMBER = "ph",
    	PARAM_VALIDATION_CODE = "valid",
    	PARAM_UPDATED = "timestamp",
    	USER_AGENT = "AuthenticationService/1.0",
    	BASE_URL = "http://10.9.8.29:8080/phonebook",
    	AUTH_URI = BASE_URL + "/mobile/index",
    	REG_URL = BASE_URL + "/mobile/register",
    	VALIDATION_URI = BASE_URL + "/mobile/validate",
    	NEW_VALIDATION_CODE_URI = BASE_URL + "/mobile/newValidationCode",
    	CHECK_VALID_ACCOUNT_URI = BASE_URL + "/mobile/valid",
    	FETCH_FRIEND_UPDATES_URI = BASE_URL + "/mobile/contacts",
        SEND_CONTACT_UPDATES_URI = BASE_URL + "/mobile/updateContacts",
        SEND_GROUP_UPDATES_URI = BASE_URL + "/mobile/updateGroups",
    	SEND_SHARED_BOOK_UPDATES_URI = BASE_URL + "/mobile/updateSharedBooks",
    	UPLOAD_CONTACT_PIC_URI = BASE_URL + "/fileUploader/process",
    	DOWNLOAD_CONTACT_PIC_URI = BASE_URL + "/download/index/";

	public Cloud(Context context, Account account, String authtoken) throws AuthenticationException, ParseException, JSONException, IOException {
		this.context = context;
		accountName = account.name;
		authToken = authtoken;
        Object[] update = fetchFriendUpdates();
        new SyncManager(context, account.name, update);
        sendFriendUpdates(true);
		
	}

    Object[] fetchFriendUpdates() throws JSONException, ParseException, IOException, AuthenticationException 
    {
    	final List<Contact> friendList = new ArrayList<Contact>();
        final List<Group> groupsList = new ArrayList<Group>();
        final List<Group> books = new ArrayList<Group>();
        List<NameValuePair> params = getAuthParams();
        final JSONArray update = new JSONArray(post(FETCH_FRIEND_UPDATES_URI, params)),
        	friends = update.getJSONArray(0),
        	groups = update.getJSONArray(1),
        	sharedBooks = update.getJSONArray(2);
        for (int i = 0; i < friends.length(); i++) 
            friendList.add(Contact.valueOf(friends.getJSONObject(i)));
        Log.i(tag, "Contacts: "+friendList);
        for (int i = 0; i < groups.length(); i++) 
            groupsList.add(Group.valueOf(groups.getJSONObject(i)));
        for (int i = 0; i < sharedBooks.length(); i++)  
            books.add(Group.valueOf(sharedBooks.getJSONObject(i)));
        
        return new Object[] {friendList, groupsList, books}; 
    }

	public void sendFriendUpdates(boolean newOnly) throws ClientProtocolException, IOException, JSONException {
		Cursor rawContactsCursor = Db.getRawContactsCursor(context.getContentResolver(), false);
		sendContactUpdates(Db.getContacts(newOnly, context), newOnly, rawContactsCursor);
        sendGroupUpdates(Db.getGroups(newOnly, context));
        sendSharedBookUpdates(Db.getSharingBooks(true, context));
        rawContactsCursor.close();
	}

	private void sendContactUpdates(List<PhoneContact> contacts, boolean newOnly, Cursor rawContactsCursor) throws ClientProtocolException, IOException, JSONException {
        List<NameValuePair> params = getAuthParams();
        params.add(new BasicNameValuePair("numContacts", new Integer(contacts.size()).toString()));
        for(int i=0; i< contacts.size(); i++)
        {
        	String index = new Integer(i).toString();
        	PhoneContact contact =  contacts.get(i);
        	contact.addParams(params, index);
        }
		
        JSONArray contactUpdates = new JSONArray(post(SEND_CONTACT_UPDATES_URI, params));
        for (int i = 0; i < contactUpdates.length(); i++)
        	ContactManager.updateContact(contactUpdates.getJSONObject(i), context, rawContactsCursor);
        // send the profile pictures here
        sendContactPictureUpdates(newOnly);
        ContactManager.resetDirtyContacts(context);
	}
	
	
	private void sendContactPictureUpdates(boolean newOnly) {
    	List<ContactPicture> pics = Db.getContactPictures(context, newOnly);
		Map<String, String> params = new HashMap<String, String>();
		params.put("upload", "avatar");
		params.put("errorAction", "error");
		params.put("errorController", "file");
		params.put("successAction", "success");
		params.put("successController", "file");

    	for(ContactPicture pic : pics)
    	{
    		String contentType = pic.mimeType.split("/")[1];
    		Log.i(tag, "uploading "+contentType);
    		params.remove("id");
    		params.put("id", pic.serverId);
    		Net.upload(Net.UPLOAD_CONTACT_PIC_URI, pic.pic, contentType, params);
    	}
	}

	private void sendGroupUpdates(List<Group> groups) throws ClientProtocolException, IOException, JSONException {
        List<NameValuePair> params = getAuthParams();
        params.add(new BasicNameValuePair("numBooks", new Integer(groups.size()).toString()));
        for(int i=0; i< groups.size(); i++)
        {
        	String index = new Integer(i).toString();
        	Group group =  groups.get(i);
        	params.add(new BasicNameValuePair("groupId_"+index, group.groupId));
        	params.add(new BasicNameValuePair("serverId_"+index, group.serverId));
        	params.add(new BasicNameValuePair("bookName_"+index, group.name));
        	List<Contact> bookContacts = group.contacts;
        	params.add(new BasicNameValuePair("numContacts_"+index, new Integer(bookContacts.size()).toString()));
        	for(int j=0; j< bookContacts.size(); j++)
        	{
        		Contact bContact = bookContacts.get(j);
        		String cIndex = new Integer(j).toString();
        		params.add(new BasicNameValuePair("serverId_"+index+"_"+cIndex, bContact.serverId));
        	}
        }
        JSONArray groupUpdates = new JSONArray(post(SEND_GROUP_UPDATES_URI, params));
        for (int i = 0; i < groupUpdates.length(); i++)
        	ContactManager.updateGroup(groupUpdates.getJSONObject(i), context);
        ContactManager.resetDirtyGroups(context);

	}
	
	private void sendSharedBookUpdates(List<SharingBook> books) throws ClientProtocolException, IOException, JSONException {
        List<NameValuePair> params = getAuthParams();
        params.add(new BasicNameValuePair("numShareBooks", new Integer(books.size()).toString()));
        for(int i=0; i< books.size(); i++)
        {
        	String index = new Integer(i).toString();
        	SharingBook book =  books.get(i);
        	params.add(new BasicNameValuePair("shareBookId_"+index, book.groupId));
        	params.add(new BasicNameValuePair("shareContactId_"+index, book.contactId));
        }
        JSONArray  bookUpdates = new JSONArray(post(SEND_SHARED_BOOK_UPDATES_URI, params));
        for (int i = 0; i < bookUpdates.length(); i++)
        	ContactManager.updateBook(bookUpdates.getJSONObject(i), context);
        ContactManager.resetDirtySharedBooks(context);
	}
	
	public String post(String url, List<NameValuePair> params) throws ClientProtocolException, IOException, JSONException {
        HttpEntity entity = new UrlEncodedFormEntity(params);
        final HttpPost post = new HttpPost(url);
        Log.i(tag, "Sending to: "+url);
        post.addHeader(entity.getContentType());
        post.setEntity(entity);
        maybeCreateHttpClient();
        final HttpResponse resp = httpClient.execute(post);
        final String response = EntityUtils.toString(resp.getEntity());
        Log.i(tag, "Response is: "+response);
        return response;
	}
	
    /**
     * Configures the httpClient to connect to the URL provided.
     */
    void maybeCreateHttpClient() {
        if (httpClient == null) {
            httpClient = new DefaultHttpClient();
            final HttpParams params = httpClient.getParams();
            HttpConnectionParams.setConnectionTimeout(params, REGISTRATION_TIMEOUT);
            HttpConnectionParams.setSoTimeout(params, REGISTRATION_TIMEOUT);
            ConnManagerParams.setTimeout(params, REGISTRATION_TIMEOUT);
        }
    }
	
	private List<NameValuePair> getAuthParams() {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_USERNAME, accountName));
        params.add(new BasicNameValuePair(PARAM_PASSWORD, authToken));
        /*if (lastUpdated != null) {
        final SimpleDateFormat formatter =
            new SimpleDateFormat("yyyy/MM/dd HH:mm");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        params.add(new BasicNameValuePair(PARAM_UPDATED, formatter
            .format(lastUpdated)));
    }
    Log.i(TAG, params.toString());*/
        
        return params;
	}
	
}
