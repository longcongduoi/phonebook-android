/*
 * Copyright (C) 2010 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.nbos.phonebook.sync.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
import org.json.JSONObject;

import android.accounts.Account;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.nbos.phonebook.ValidationActivity;
import com.nbos.phonebook.sync.authenticator.AuthenticatorActivity;
import com.nbos.phonebook.sync.platform.Cloud;
import com.nbos.phonebook.sync.platform.ContactManager;

/**
 * Provides utility methods for communicating with the server.
 */
public class Net {
    private static final String tag = "NetworkUtilities";
    public static final String PARAM_USERNAME = "username";
    public static final String PARAM_PASSWORD = "password";
    public static final String PARAM_PHONE_NUMBER = "ph";
    public static final String PARAM_VALIDATION_CODE = "valid";
    public static final String PARAM_UPDATED = "timestamp";
    public static final String USER_AGENT = "AuthenticationService/1.0";
    public static final int REGISTRATION_TIMEOUT = 30 * 1000; // ms
    public static final String BASE_URL = Cloud.BASE_URL;
    public static final String AUTH_URI = BASE_URL + "/mobile/index",
    	REG_URL = BASE_URL + "/mobile/register",
    	VALIDATION_URI = BASE_URL + "/mobile/validate",
    	NEW_VALIDATION_CODE_URI = BASE_URL + "/mobile/newValidationCode";
    public static final String 
    	CHECK_VALID_ACCOUNT_URI = BASE_URL + "/mobile/valid",
    	FETCH_FRIEND_UPDATES_URI = BASE_URL + "/mobile/contacts",
        SEND_CONTACT_UPDATES_URI = BASE_URL + "/mobile/updateContacts",
        SEND_GROUP_UPDATES_URI = BASE_URL + "/mobile/updateGroups",
    	SEND_SHARED_BOOK_UPDATES_URI = BASE_URL + "/mobile/updateSharedBooks",
    	DOWNLOAD_CONTACT_PIC_URI = BASE_URL + "/download/index/";
    // String urlServer = "http://10.9.8.29:8080/phonebook/fileUploader/process";
    public static final String FETCH_STATUS_URI = BASE_URL + "/fetch_status";
    private static HttpClient mHttpClient;

    /**
     * Configures the httpClient to connect to the URL provided.
     */
    public static void maybeCreateHttpClient() {
        if (mHttpClient == null) {
            mHttpClient = new DefaultHttpClient();
            final HttpParams params = mHttpClient.getParams();
            HttpConnectionParams.setConnectionTimeout(params,
                REGISTRATION_TIMEOUT);
            HttpConnectionParams.setSoTimeout(params, REGISTRATION_TIMEOUT);
            ConnManagerParams.setTimeout(params, REGISTRATION_TIMEOUT);
        }
    }

    /**
     * Executes the network requests on a separate thread.
     * 
     * @param runnable The runnable instance containing network mOperations to
     *        be executed.
     */
    public static Thread performOnBackgroundThread(final Runnable runnable) {
        final Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } finally {

                }
            }
        };
        t.start();
        return t;
    }

    /**
     * Connects to the Voiper server, authenticates the provided username and
     * password.
     * 
     * @param username The user's username
     * @param password The user's password
     * @param handler The hander instance from the calling UI thread.
     * @param context The context of the calling Activity.
     * @return boolean The boolean result indicating whether the user was
     *         successfully authenticated.
     */
    public static boolean authenticate(String username, String password, String ph,
        Handler handler, final Context context) {
    	Log.i(tag, "Authenticate");
        final HttpResponse resp;

        final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_USERNAME, username));
        params.add(new BasicNameValuePair(PARAM_PASSWORD, password));
        params.add(new BasicNameValuePair(PARAM_PHONE_NUMBER, ph));
        HttpEntity entity = null;
        try {
            entity = new UrlEncodedFormEntity(params);
        } catch (final UnsupportedEncodingException e) {
            // this should never happen.
            throw new AssertionError(e);
        }
        
        final HttpPost post = new HttpPost(AUTH_URI);
        post.addHeader(entity.getContentType());
        post.setEntity(entity);
        maybeCreateHttpClient();
        Log.i(tag, "Posting to: "+AUTH_URI);

        try {
            resp = mHttpClient.execute(post);
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                // if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.i(tag, "Successful authentication");
                    Log.i(tag, "data: "+EntityUtils.toString(resp.getEntity()));
                //}
                sendResult(true, handler, context, null);
                return true;
            } else {
                // if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(tag, "Error authenticating" + resp.getStatusLine());
                // }
                sendResult(false, handler, context, "Error authenticating" + resp.getStatusLine());
                return false;
            }
        } catch (final IOException e) {
            // if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(tag, "Exception: ", e);
            // }
            sendResult(false, handler, context, e.getMessage());
            return false;
        } finally {
            // if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(tag, "getAuthtoken completing");
            //}
        }
    }

    public static boolean register(String username, String password, String ph,
            Handler handler, final Context context) {
        	Log.i(tag, "Register" +" ,ph:"+ph);
            final HttpResponse resp;

            final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair(PARAM_USERNAME, username));
            params.add(new BasicNameValuePair(PARAM_PASSWORD, password));
            params.add(new BasicNameValuePair(PARAM_PHONE_NUMBER, ph));
            HttpEntity entity = null;
            try {
                entity = new UrlEncodedFormEntity(params);
            } catch (final UnsupportedEncodingException e) {
                // this should never happen.
                throw new AssertionError(e);
            }
            
            final HttpPost post = new HttpPost(REG_URL);
            post.addHeader(entity.getContentType());
            post.setEntity(entity);
            maybeCreateHttpClient();
            Log.i(tag, "Posting to: "+REG_URL);

            try {
                resp = mHttpClient.execute(post);
                if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    // if (Log.isLoggable(TAG, Log.VERBOSE)) {
                        Log.i(tag, "Successful authentication");
                        Log.i(tag, "data: "+EntityUtils.toString(resp.getEntity()));
                    //}
                    sendResult(true, handler, context, null);
                    return true;
                } else {
                    // if (Log.isLoggable(TAG, Log.VERBOSE)) {
                        Log.v(tag, "Error authenticating" + resp.getStatusLine());
                    // }
                    sendResult(false, handler, context, resp.getStatusLine().toString());
                    return false;
                }
            } catch (final IOException e) {
                // if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(tag, "IOException when getting authtoken", e);
                // }
                sendResult(false, handler, context, e.getMessage());
                return false;
            } finally {
                // if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(tag, "getAuthtoken completing");
                //}
            }
        }

    public static boolean validate(String username, String password, String ph, String validationCode,
            Handler handler, final Context context) {
        	Log.i(tag, "Validate, ph: "+ph+", code: "+validationCode);
            final HttpResponse resp;

            final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair(PARAM_USERNAME, username));
            params.add(new BasicNameValuePair(PARAM_PASSWORD, password));
            params.add(new BasicNameValuePair(PARAM_PHONE_NUMBER, ph));
            params.add(new BasicNameValuePair(PARAM_VALIDATION_CODE, validationCode));
            HttpEntity entity = null;
            try {
                entity = new UrlEncodedFormEntity(params);
            } catch (final UnsupportedEncodingException e) {
                // this should never happen.
                throw new AssertionError(e);
            }
            
            final HttpPost post = new HttpPost(VALIDATION_URI);
            post.addHeader(entity.getContentType());
            post.setEntity(entity);
            maybeCreateHttpClient();
            Log.i(tag, "Posting to: "+VALIDATION_URI);

            try {
                resp = mHttpClient.execute(post);
                if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                	String response = EntityUtils.toString(resp.getEntity());
                	Log.i(tag, "Successful authentication");
                    Log.i(tag, "data: "+response);
                    JSONObject result = new JSONObject(response);
                    boolean validated = result.getBoolean("validated");
                    if(validated)
                    	sendValidationResult(true, "Validation was successful", handler, context);
                    else
                    	sendValidationResult(false, "Validation was not successful", handler, context);
                    return true;
                } else {
                    // if (Log.isLoggable(TAG, Log.VERBOSE)) {
                        Log.v(tag, "Error authenticating: " + resp.getStatusLine());
                    // }
                    sendValidationResult(false, "Error authenticating: " + resp.getStatusLine(), handler, context);
                    return false;
                }
            } catch (final Exception e) {
                Log.v(tag, "IOException when getting authtoken", e);
                sendValidationResult(false, "Exception: "+e.getMessage(), handler, context);
                return false;
            } 
        }

    
    public static boolean newValidateCode(String username, String password, String ph,
            Handler handler, final Context context) {
        	Log.i(tag, "Validate, ph: "+ph);
            final HttpResponse resp;

            final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair(PARAM_USERNAME, username));
            params.add(new BasicNameValuePair(PARAM_PASSWORD, password));
            params.add(new BasicNameValuePair(PARAM_PHONE_NUMBER, ph));
            HttpEntity entity = null;
            try {
                entity = new UrlEncodedFormEntity(params);
            } catch (final UnsupportedEncodingException e) {
                // this should never happen.
                throw new AssertionError(e);
            }
            
            final HttpPost post = new HttpPost(NEW_VALIDATION_CODE_URI);
            post.addHeader(entity.getContentType());
            post.setEntity(entity);
            maybeCreateHttpClient();
            Log.i(tag, "Posting to: "+NEW_VALIDATION_CODE_URI);

            try {
                resp = mHttpClient.execute(post);
                if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                	String response = EntityUtils.toString(resp.getEntity());
                	Log.i(tag, "Successful authentication");
                    Log.i(tag, "data: "+response);
                    sendNewValidationCodeResult(true, "New validation code was sent to "+ph, handler, context);
                    return true;
                } else {
                    Log.v(tag, "Error authenticating: " + resp.getStatusLine());
                    sendNewValidationCodeResult(false, "Error authenticating: " + resp.getStatusLine(), handler, context);
                    return false;
                }
            } catch (final Exception e) {
                Log.v(tag, "Exception: ", e);
                sendNewValidationCodeResult(false, "Exception: "+e.getMessage(), handler, context);
                return false;
            } 
        }
    
    /**
     * Sends the authentication response from server back to the caller main UI
     * thread through its handler.
     * 
     * @param result The boolean holding authentication result
     * @param handler The main UI thread's handler instance.
     * @param context The caller Activity's context.
     */
    private static void sendResult(final Boolean result, final Handler handler,
        final Context context, final String message) {
    	Log.i(tag, "sendResult("+result+")");
        if (handler == null || context == null) {
            return;
        }
        handler.post(new Runnable() {
            public void run() {
                ((AuthenticatorActivity) context).onAuthenticationResult(result, message);
            }
        });
    }

    private static void sendValidationResult(final Boolean result, final String message, final Handler handler,
            final Context context) {
        	Log.i(tag, "sendValidationResult("+result+")");
            if (handler == null || context == null) {
                return;
            }
            handler.post(new Runnable() {
                public void run() {
                    ((ValidationActivity) context).onValidationResult(result, message);
                }
            });
        }

    private static void sendNewValidationCodeResult(final Boolean result, final String message, final Handler handler,
            final Context context) {
        	Log.i(tag, "sendValidationResult("+result+")");
            if (handler == null || context == null) {
                return;
            }
            handler.post(new Runnable() {
                public void run() {
                    ((ValidationActivity) context).onNewValidationCodeResult(result, message);
                }
            });
        }

    /**
     * Attempts to authenticate the user credentials on the server.
     * 
     * @param username The user's username
     * @param password The user's password to be authenticated
     * @param handler The main UI thread's handler instance.
     * @param context The caller Activity's context
     * @return Thread The thread on which the network mOperations are executed.
     */
    public static Thread attemptAuth(final String username,
        final String password, final String ph, final Handler handler, final Context context) {
        final Runnable runnable = new Runnable() {
            public void run() {
                authenticate(username, password, ph, handler, context);
            }
        };
        // run on background thread.
        return Net.performOnBackgroundThread(runnable);
    }

    public static Thread attemptRegister(final String username,
            final String password, final String ph, final Handler handler, final Context context) {
            final Runnable runnable = new Runnable() {
                public void run() {
                    register(username, password, ph, handler, context);
                }
            };
            // run on background thread.
            return Net.performOnBackgroundThread(runnable);
        }
    
    /**
     * Fetches the list of friend data updates from the server
     * 
     * @param account The account being synced.
     * @param authtoken The authtoken stored in AccountManager for this account
     * @param lastUpdated The last time that sync was performed
     * @return list The list of updates received from the server.
     */
    public static Object[] fetchFriendUpdates(Account account,
        String authtoken, Date lastUpdated) throws JSONException,
        ParseException, IOException, AuthenticationException {
        final List<Contact> friendList = new ArrayList<Contact>();
        final List<Group> groupsList = new ArrayList<Group>();
        final List<Group> books = new ArrayList<Group>();
        final List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_USERNAME, account.name));
        params.add(new BasicNameValuePair(PARAM_PASSWORD, authtoken));
        // params.add(new BasicNameValuePair(PARAM_PHONE_NUMBER, phoneNumber.toString()));
        if (lastUpdated != null) {
            final SimpleDateFormat formatter =
                new SimpleDateFormat("yyyy/MM/dd HH:mm");
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            params.add(new BasicNameValuePair(PARAM_UPDATED, formatter
                .format(lastUpdated)));
        }
        Log.i(tag, params.toString());

        HttpEntity entity = null;
        entity = new UrlEncodedFormEntity(params);
        final HttpPost post = new HttpPost(FETCH_FRIEND_UPDATES_URI);
        Log.i(tag, "Fetching friends from: "+FETCH_FRIEND_UPDATES_URI);
        post.addHeader(entity.getContentType());
        post.setEntity(entity);
        maybeCreateHttpClient();

        final HttpResponse resp = mHttpClient.execute(post);
        final String response = EntityUtils.toString(resp.getEntity());

        if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            // Succesfully connected to the samplesyncadapter server and
            // authenticated.
            // Extract friends data in json format.
            final JSONArray update = new JSONArray(response),
            	friends = update.getJSONArray(0),
            	groups = update.getJSONArray(1),
            	sharedBooks = update.getJSONArray(2);
            Log.d(tag, "response: "+response);
            for (int i = 0; i < friends.length(); i++) {
                // friendList.add(Contact.valueOf(friends.getJSONObject(i)));
            }

            for (int i = 0; i < groups.length(); i++) {
                groupsList.add(Group.valueOf(groups.getJSONObject(i)));
            }
            
            Log.i(tag, "There are "+sharedBooks.length()+" shared books");
            for (int i = 0; i < sharedBooks.length(); i++) { 
                books.add(Group.valueOf(sharedBooks.getJSONObject(i)));
            }

        } else {
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                Log.e(tag,
                    "Authentication exception in fetching remote contacts");
                throw new AuthenticationException();
            } else {
                Log.e(tag, "Server error in fetching remote contacts: "
                    + resp.getStatusLine());
                Log.e(tag, response);
                throw new IOException();
            }
        }
        Object [] update = {friendList, groupsList, books}; 
        return update;
    }


	/**
     * Fetches status messages for the user's friends from the server
     * 
     * @param account The account being synced.
     * @param authtoken The authtoken stored in the AccountManager for the
     *        account
     * @return list The list of status messages received from the server.
     */
    public static List<User.Status> fetchFriendStatuses(Account account,
        String authtoken) throws JSONException, ParseException, IOException,
        AuthenticationException {
        final ArrayList<User.Status> statusList = new ArrayList<User.Status>();
        final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_USERNAME, account.name));
        params.add(new BasicNameValuePair(PARAM_PASSWORD, authtoken));

        HttpEntity entity = null;
        entity = new UrlEncodedFormEntity(params);
        final HttpPost post = new HttpPost(FETCH_STATUS_URI);
        post.addHeader(entity.getContentType());
        post.setEntity(entity);
        maybeCreateHttpClient();

        final HttpResponse resp = mHttpClient.execute(post);
        final String response = EntityUtils.toString(resp.getEntity());

        if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            // Succesfully connected to the samplesyncadapter server and
            // authenticated.
            // Extract friends data in json format.
            final JSONArray statuses = new JSONArray(response);
            for (int i = 0; i < statuses.length(); i++) {
                statusList.add(User.Status.valueOf(statuses.getJSONObject(i)));
            }
        } else {
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                Log.e(tag,
                    "Authentication exception in fetching friend status list");
                throw new AuthenticationException();
            } else {
                Log.e(tag, "Server error in fetching friend status list");
                throw new IOException();
            }
        }
        return statusList;
    }

    static Context mContext;
    static String accountName, authToken;
	
	private static void sendSharedBookUpdates(List<SharingBook> books) throws ClientProtocolException, IOException, JSONException {
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
        	ContactManager.updateBook(bookUpdates.getJSONObject(i), mContext);
        ContactManager.resetDirtySharedBooks(mContext);
	}

	private static void sendGroupUpdates(List<Group> groups) throws ClientProtocolException, IOException, JSONException {
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
        		// params.add(new BasicNameValuePair("contactId_"+index+"_"+cIndex, bContact.contactId));
        		params.add(new BasicNameValuePair("serverId_"+index+"_"+cIndex, bContact.serverId));
        	}
        }
        JSONArray groupUpdates = new JSONArray(post(SEND_GROUP_UPDATES_URI, params));
        for (int i = 0; i < groupUpdates.length(); i++)
        	ContactManager.updateGroup(groupUpdates.getJSONObject(i), mContext);
        ContactManager.resetDirtyGroups(mContext);

	}

	public static boolean checkValidAccount(Account account, String authtoken, String phone) throws ClientProtocolException, JSONException, IOException {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_USERNAME, account.name));
        params.add(new BasicNameValuePair(PARAM_PASSWORD, authtoken));
        params.add(new BasicNameValuePair(PARAM_PHONE_NUMBER, phone));
        JSONObject response = new JSONObject(post(CHECK_VALID_ACCOUNT_URI, params));
        return response.getBoolean("valid");
	}

	private static List<NameValuePair> getAuthParams() {
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

	public static String post(String url, List<NameValuePair> params) throws ClientProtocolException, IOException, JSONException {
        HttpEntity entity = new UrlEncodedFormEntity(params);
        final HttpPost post = new HttpPost(url);
        Log.i(tag, "Sending to: "+url);
        post.addHeader(entity.getContentType());
        post.setEntity(entity);
        maybeCreateHttpClient();
        final HttpResponse resp = mHttpClient.execute(post);
        final String response = EntityUtils.toString(resp.getEntity());
        Log.i(tag, "Response is: "+response);
        return response;
	}

	public static Thread attemptValidate(final String userName, final String password,
			final String phoneNumber, final String validationCode, final Handler handler, final Context context) {
        final Runnable runnable = new Runnable() {
            public void run() {
                validate(userName, password, phoneNumber, validationCode, handler, context);
            }
        };
        // run on background thread.
        return Net.performOnBackgroundThread(runnable);
	}

	public static Thread attemptNewValidateCode(final String userName,
			final String password, final String phoneNumber, final Handler handler,
			final Context context) {
        final Runnable runnable = new Runnable() {
            public void run() {
                newValidateCode(userName, password, phoneNumber, handler, context);
            }
        };
        // run on background thread.
        return Net.performOnBackgroundThread(runnable);
	}
}
