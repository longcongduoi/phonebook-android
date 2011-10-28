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

package com.nbos.phonebook.sync.authenticator;

import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.facebook.android.Util;
import com.facebook.android.Facebook.DialogListener;
import com.nbos.phonebook.Db;
import com.nbos.phonebook.R;
import com.nbos.phonebook.sync.Constants;
import com.nbos.phonebook.sync.client.Net;

/**
 * Activity which displays login screen to the user.
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity {
    public static final String PARAM_CONFIRMCREDENTIALS = "confirmCredentials";
    public static final String PARAM_USERNAME = "username";
    public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";

    private static final String TAG = "AuthenticatorActivity";

    private AccountManager mAccountManager;
    private Thread mAuthThread;
    private String mAuthtoken;
    private String mAuthtokenType;
    Facebook facebook = new Facebook("206032716132433");
    /**
     * If set we are just checking that the user knows their credentials; this
     * doesn't cause the user's password to be changed on the device.
     */
    private Boolean mConfirmCredentials = false;

    /** for posting authentication attempts back to UI thread */
    private final Handler mHandler = new Handler();
    private TextView mMessage;
    private String mPassword, mPhone;
    private EditText mPasswordEdit, mPhoneEdit;

    /** Was the original caller asking for an entirely new account? */
    protected boolean mRequestNewAccount = false;

    private String mUsername;
    private EditText mUsernameEdit;
    private String tag="Facebook";
    String FILENAME = "Androidphonebook_data";
    private SharedPreferences mPrefs;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle icicle) {
        Log.i(TAG, "onCreate(" + icicle + ")");
        super.onCreate(icicle);
        mAccountManager = AccountManager.get(this);
        Log.i(TAG, "loading data from Intent");
        final Intent intent = getIntent();
        mUsername = intent.getStringExtra(PARAM_USERNAME);
        mAuthtokenType = intent.getStringExtra(PARAM_AUTHTOKEN_TYPE);
        mRequestNewAccount = mUsername == null;
        mConfirmCredentials =
            intent.getBooleanExtra(PARAM_CONFIRMCREDENTIALS, false);

        Log.i(TAG, "request new: " + mRequestNewAccount);
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.login_activity);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
            android.R.drawable.ic_dialog_alert);

        mMessage = (TextView) findViewById(R.id.message);
        mUsernameEdit = (EditText) findViewById(R.id.username_edit);
        mPasswordEdit = (EditText) findViewById(R.id.password_edit);
        mPhoneEdit = (EditText) findViewById(R.id.phone_edit);
        getPhoneNumber(getApplicationContext());
        mUsernameEdit.setText(mUsername);
        mMessage.setText(getMessage());
    }

    private void getPhoneNumber(Context context) {
    	String ph = Db.getPhoneNumber(context);
    	if(ph == null) return;
    	mPhoneEdit.setText(ph);
    	// mPhoneEdit.setFocusable(false);
	}

	/*
     * {@inheritDoc}
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(getText(R.string.ui_activity_authenticating));
        dialog.setIndeterminate(true);
        dialog.setCancelable(true);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                Log.i(TAG, "dialog cancel has been invoked");
                if (mAuthThread != null) {
                    mAuthThread.interrupt();
                    finish();
                }
            }
        });
        return dialog;
    }
    
    /**
     * Handles onClick event on the Submit button. Sends username/password to
     * the server for authentication.
     * 
     * @param view The Submit button for which this method is invoked
     */
    public void handleLogin(View view) {
        if (mRequestNewAccount) {
            mUsername = mUsernameEdit.getText().toString();
        }
        mPassword = mPasswordEdit.getText().toString();
        mPhone = mPhoneEdit.getText().toString();
        if (TextUtils.isEmpty(mUsername) || TextUtils.isEmpty(mPassword) || TextUtils.isEmpty(mPhone)) {
            mMessage.setText(getMessage());
        } else {
            showProgress();
            // Start authenticating...
            mAuthThread =
                Net.attemptAuth(mUsername, mPassword, mPhone, mHandler,
                    AuthenticatorActivity.this);
        }
    }

    public void handleRegister(View view) {
    	Log.i(TAG, "handleRegister()");
        if (mRequestNewAccount) {
            mUsername = mUsernameEdit.getText().toString();
        }
        mPassword = mPasswordEdit.getText().toString();
        mPhone = mPhoneEdit.getText().toString();
        Log.i(TAG, "usename: "+mUsername+", password: "+mPassword+", mPhone: "+mPhone);
        if (TextUtils.isEmpty(mUsername) || TextUtils.isEmpty(mPassword) || TextUtils.isEmpty(mPhone)) {
        	Log.i(TAG, "Empty field");
            mMessage.setText(getMessage());
        } else {
            showProgress();
            // Start authenticating...
            Log.i(TAG, "attempting register");
            mAuthThread =
                Net.attemptRegister(mUsername, mPassword, mPhone, mHandler,
                    AuthenticatorActivity.this);
        }
    }

    /**
     * Called when response is received from the server for confirm credentials
     * request. See onAuthenticationResult(). Sets the
     * AccountAuthenticatorResult which is sent back to the caller.
     * 
     * @param the confirmCredentials result.
     */
    protected void finishConfirmCredentials(boolean result) {
        Log.i(TAG, "finishConfirmCredentials()");
        final Account account = new Account(mUsername, Constants.ACCOUNT_TYPE);
        mAccountManager.setPassword(account, mPassword);
        mAccountManager.setUserData(account, Constants.PHONE_NUMBER_KEY, mPhone);
        final Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_BOOLEAN_RESULT, result);
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * 
     * Called when response is received from the server for authentication
     * request. See onAuthenticationResult(). Sets the
     * AccountAuthenticatorResult which is sent back to the caller. Also sets
     * the authToken in AccountManager for this account.
     * 
     * @param the confirmCredentials result.
     */

    protected void finishLogin() {
        Log.i(TAG, "finishLogin()");
        final Account account = new Account(mUsername, Constants.ACCOUNT_TYPE);

        if (mRequestNewAccount) {
            mAccountManager.addAccountExplicitly(account, mPassword, null);
            mAccountManager.setUserData(account, Constants.PHONE_NUMBER_KEY, mPhone);            
            // Set contacts sync for this account.
            ContentResolver.setSyncAutomatically(account,
                ContactsContract.AUTHORITY, true);
        } else {
            mAccountManager.setPassword(account, mPassword);
        }
        final Intent intent = new Intent();
        mAuthtoken = mPassword;
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mUsername);
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
        if (mAuthtokenType != null
            && mAuthtokenType.equals(Constants.AUTHTOKEN_TYPE)) {
            intent.putExtra(AccountManager.KEY_AUTHTOKEN, mAuthtoken);
        }
        Db.deleteServerData(getApplicationContext());
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * Hides the progress UI for a lengthy operation.
     */
    protected void hideProgress() {
        dismissDialog(0);
    }

    /**
     * Called when the authentication process completes (see attemptLogin()).
     */
    public void onAuthenticationResult(boolean result, String message) {
        Log.i(TAG, "onAuthenticationResult(" + result + ")");
        // Hide the progress dialog
        hideProgress();
        if (result) {
            if (!mConfirmCredentials) {
                finishLogin();
            } else {
                finishConfirmCredentials(true);
            }
            /*try {
            	Log.i(TAG, "Sending all contacts");
            	// Db.refreshAccount(getApplicationContext(), mUsername);
				// Net.sendAllContacts(mUsername, this.mAuthtoken, getApplicationContext());
                final Runnable runnable = new Runnable() {
                    public void run() {
                    	try {
							new Cloud(getApplicationContext(), mUsername, mAuthtoken).sendAllContacts();
						} catch (Exception e) {
							e.printStackTrace();
						}
                    }
                };
                Net.performOnBackgroundThread(runnable);				
			} catch (Exception e) {
				e.printStackTrace();
			}*/
            Log.i(TAG, "finish authentication");
        } else {
            Log.e(TAG, "onAuthenticationResult: failed to authenticate - "+message);
            mMessage.setText(message);
            /*if (mRequestNewAccount) {
                // "Please enter a valid username/password.
                mMessage.setText(getText(R.string.login_activity_loginfail_text_both));
            } else {
                // "Please enter a valid password." (Used when the
                // account is already in the database but the password
                // doesn't work.)
                mMessage.setText(getText(R.string.login_activity_loginfail_text_pwonly));
            }*/
        }
    }

    /**
     * Returns the message to be displayed at the top of the login dialog box.
     */
    private CharSequence getMessage() {
        getString(R.string.label);
        if (TextUtils.isEmpty(mUsername)) {
            // If no username, then we ask the user to log in using an
            // appropriate service.
            final CharSequence msg =
                getText(R.string.login_activity_newaccount_text);
            return msg;
        }
        if (TextUtils.isEmpty(mPassword)) {
            // We have an account but no password
            return getText(R.string.login_activity_loginfail_text_pwmissing);
        }
        if (TextUtils.isEmpty(mPhone)) {
            // We have an account but no password
            return getText(R.string.login_activity_loginfail_text_phmissing);
        }

        return null;
    }

    /**
     * Shows the progress UI for a lengthy operation.
     */
    protected void showProgress() {
        showDialog(0);
    }
    public void registerAsFbUser(View v){
    	if(!facebook.isSessionValid()) {
    	facebook.authorize(this, new DialogListener() {
            public void onComplete(Bundle values) {
            	
            }

            public void onFacebookError(FacebookError error) {}

            public void onError(DialogError e) {}

            public void onCancel() {}
        });
      }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
         facebook.authorizeCallback(requestCode, resultCode, data);
         View v=(View)findViewById(R.id.login_layout);
         createAccountWithFbId(v);
       
    }
    public void createAccountWithFbId(View v){
   	 mPrefs = getPreferences(MODE_PRIVATE);
   	 SharedPreferences.Editor editor = mPrefs.edit();
     	editor.putString("access_token", facebook.getAccessToken());
        editor.putLong("access_expires", facebook.getAccessExpires());
        editor.commit();
        String access_token = mPrefs.getString("access_token", null);
        long expires = mPrefs.getLong("access_expires", 0);
        if(access_token != null) {
            facebook.setAccessToken(access_token);
        }
        if(expires != 0) {
            facebook.setAccessExpires(expires);
        }
        
        /*
         * Only call authorize if the access_token has expired.
         */
        if(facebook.isSessionValid()) {
        	Log.i(tag, "Session is valid");
        	JSONObject json;
			try {
				json = Util.parseJson(facebook.request("me", new Bundle()));
				String userId = json.getString("id"),
				userName=json.getString("first_name");
				mUsernameEdit.setText(userName);
				mPasswordEdit.setText(userId);
				Log.i(tag, "userName: "+userName+" userId: "+userId);
				Log.i(tag,"response:"+json);
			} catch (Exception e1) {
				e1.printStackTrace();
			} catch (FacebookError e) {
				e.printStackTrace();
			}
        }
        
   }
}
