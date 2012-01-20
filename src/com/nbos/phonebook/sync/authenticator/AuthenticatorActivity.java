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
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;
import com.facebook.android.Util;
import com.nbos.phonebook.Db;
import com.nbos.phonebook.R;
import com.nbos.phonebook.sync.Constants;
import com.nbos.phonebook.sync.client.Net;
import com.nbos.phonebook.sync.platform.Cloud;
import com.nbos.phonebook.util.CountryMap;
import com.nbos.phonebook.util.Text;

/**
 * Activity which displays login screen to the user.
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity implements OnItemSelectedListener{
    public static final String PARAM_CONFIRMCREDENTIALS = "confirmCredentials";
    public static final String PARAM_USERNAME = "username";
    public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";

    private static final String tag = "AuthenticatorActivity";

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
    private String mPassword, mPhone, countryCode;
    private EditText mPasswordEdit, mPhoneEdit;

    /** Was the original caller asking for an entirely new account? */
    protected boolean mRequestNewAccount = false;

    private String mUsername;
    private EditText mUsernameEdit;
    String FILENAME = "Androidphonebook_data";
    private SharedPreferences mPrefs;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle icicle) {
        Log.i(tag, "onCreate(" + icicle + ")");
        super.onCreate(icicle);
        mAccountManager = AccountManager.get(this);
        Log.i(tag, "loading data from Intent");
        final Intent intent = getIntent();
        mUsername = intent.getStringExtra(PARAM_USERNAME);
        mAuthtokenType = intent.getStringExtra(PARAM_AUTHTOKEN_TYPE);
        mRequestNewAccount = mUsername == null;
        mConfirmCredentials =
            intent.getBooleanExtra(PARAM_CONFIRMCREDENTIALS, false);

        Log.i(tag, "request new: " + mRequestNewAccount+ " ,ConfirmCredentials: "+mConfirmCredentials);
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.login_activity);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
            android.R.drawable.ic_dialog_alert);

        Spinner spinner = (Spinner) findViewById(R.id.spinner);
	    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
	            this, R.array.country_code_array, android.R.layout.simple_spinner_item);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    spinner.setAdapter(adapter);
	    spinner.setOnItemSelectedListener(this);
	    setSpinnerCountry(spinner);
        mMessage = (TextView) findViewById(R.id.message);
        mUsernameEdit = (EditText) findViewById(R.id.username_edit);
        mPasswordEdit = (EditText) findViewById(R.id.password_edit);
        mPhoneEdit = (EditText) findViewById(R.id.phone_edit);
        getPhoneNumber(getApplicationContext());
        mUsernameEdit.setText(mUsername);
        mMessage.setText(getMessage());
    }

    private void setSpinnerCountry(Spinner spinner) {
    	TelephonyManager tel = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
		String country = tel.getSimCountryIso();
		Log.i(tag,"country: "+country);
		if(Text.isEmpty(country))
		{
		    spinner.setSelection(92); // India
		    return;
		}
		CountryMap m = new CountryMap();
		Log.i(tag,"index: "+m.getIndex(country.toUpperCase()));
		spinner.setSelection(m.getIndex(country.toUpperCase()));
	    Log.i(tag,"country: "+m.getCallingCode(country.toUpperCase()));
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
                Log.i(tag, "dialog cancel has been invoked");
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
                Net.attemptAuth(mUsername, mPassword, countryCode + mPhone, mHandler,
                    AuthenticatorActivity.this);
        }
    }

    public void handleRegister(View view) {
    	Log.i(tag, "handleRegister()");
        if (mRequestNewAccount) {
            mUsername = mUsernameEdit.getText().toString();
        }
        mPassword = mPasswordEdit.getText().toString();
        mPhone = mPhoneEdit.getText().toString();
        Log.i(tag, "usename: "+mUsername+", password: "+mPassword+", mPhone: "+mPhone);
        if (TextUtils.isEmpty(mUsername) || TextUtils.isEmpty(mPassword) || TextUtils.isEmpty(mPhone)) {
        	Log.i(tag, "Empty field");
            mMessage.setText(getMessage());
        } else {
            showProgress();
            // Start authenticating...
            Log.i(tag, "attempting register");
            mAuthThread =
                Net.attemptRegister(mUsername, mPassword, countryCode + mPhone, mHandler,
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
        Log.i(tag, "finishConfirmCredentials()");
        final Account account = new Account(mUsername, Constants.ACCOUNT_TYPE);
        mAccountManager.setPassword(account, mPassword);
        mAccountManager.setUserData(account, Constants.PHONE_NUMBER_KEY, countryCode + mPhone);
        Db.deleteServerData(getApplicationContext());
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
        Log.i(tag, "finishLogin()");
        final Account account = new Account(mUsername, Constants.ACCOUNT_TYPE);

        if (mRequestNewAccount) {
            mAccountManager.addAccountExplicitly(account, mPassword, null);
            mAccountManager.setUserData(account, Constants.PHONE_NUMBER_KEY, countryCode + mPhone);            
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
        && mAuthtokenType.equals(Constants.AUTHTOKEN_TYPE)) 
            intent.putExtra(AccountManager.KEY_AUTHTOKEN, mAuthtoken);
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
        Log.i(tag, "onAuthenticationResult(" + result + ")");
        // Hide the progress dialog
        hideProgress();
        if (result) {
            if (!mConfirmCredentials) {
                finishLogin();
            } else {
                finishConfirmCredentials(true);
            }
            Log.i(tag, "finish authentication");
        } else {
            Log.e(tag, "onAuthenticationResult: failed to authenticate - "+message);
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
            // We have an account but no phone
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
        mPhone = mPhoneEdit.getText().toString();
        if (TextUtils.isEmpty(mPhone)) {
            mMessage.setText(getText(R.string.login_activity_loginfail_text_phmissing));
            return;
        }
        Log.i(tag, "Facebook session is valid? "+facebook.isSessionValid());
    	if(!facebook.isSessionValid()) {
	    	facebook.authorize(this, new DialogListener() {
	            public void onComplete(Bundle values) {}
	            public void onFacebookError(FacebookError error) {}
	            public void onError(DialogError e) {}
	            public void onCancel() {}
	        });
	    	return;
    	}
    	createAccountWithFbId();
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        facebook.authorizeCallback(requestCode, resultCode, data);
        createAccountWithFbId();
    }
    
    void createAccountWithFbId() {
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
				userName=json.getString("name");
				mPhone = mPhoneEdit.getText().toString();
				new Cloud(getApplicationContext(), userId, userId).loginWithFacebook(countryCode + mPhone);
				mUsername = userId;
				mPassword = userId;
				finishLogin();
				// mUsernameEdit.setText(userName);
				// mPasswordEdit.setText(userId);
				Log.i(tag, "userName: "+userName+" userId: "+userId);
				Log.i(tag,"response:"+json);
			} catch (Exception e1) {
				Log.e(tag, "Exception logging on with Facebook: "+e1);
				e1.printStackTrace();
			} catch (FacebookError e) {
				Log.e(tag, "Facebook error logging on with Facebook: "+e);
				e.printStackTrace();
			}
        }
   }
    
    @Override
    public void onItemSelected(AdapterView<?> parent,
            View view, int pos, long id) 
    {
		String value = parent.getItemAtPosition(pos).toString();
		String [] parts = value.split("\\+");
		String country = parts[0].substring(0, parts[0].indexOf("(")).trim();
		countryCode = parts[1].substring(0, parts[1].indexOf(")")).trim();
		Toast.makeText(parent.getContext(), 
			"Country is " + country +", code is: "+countryCode,
			Toast.LENGTH_LONG).show();
    }

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// TODO Auto-generated method stub
		
	}
}

