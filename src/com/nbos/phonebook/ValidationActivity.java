package com.nbos.phonebook;

import java.io.IOException;

import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.json.JSONException;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.nbos.phonebook.sync.authenticator.AuthenticatorActivity;
import com.nbos.phonebook.sync.client.Net;
import com.nbos.phonebook.sync.platform.Cloud;
import com.nbos.phonebook.sync.syncadapter.SyncAdapter;

public class ValidationActivity extends Activity {

	static String TAG = "ValidationActivity";
	TextView messageText, newValidationCodeMessage;
	EditText validationCodeEdit;
	String userName, password, phoneNumber;
	private final Handler mHandler = new Handler();
	Thread mValidThread;
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.validation_activity);
        messageText = (TextView) findViewById(R.id.validation_message);
        newValidationCodeMessage = (TextView) findViewById(R.id.new_validation_message);
        validationCodeEdit = (EditText) findViewById(R.id.valid_code_edit);
        final Intent intent = getIntent();
        userName = intent.getStringExtra(Net.PARAM_USERNAME);
        password = intent.getStringExtra(Net.PARAM_PASSWORD);
        phoneNumber = intent.getStringExtra(Net.PARAM_PHONE_NUMBER);
        Log.i(TAG, "user: "+userName+", password: "+password+", phoneNumber: "+phoneNumber);
	}

	public void handleValidate(View view) {
		String validationCode = validationCodeEdit.getText().toString();
		Log.i(TAG, "Handle validate: "+validationCode);
		
        if (TextUtils.isEmpty(validationCode)) {
            messageText.setText("Please enter a validation code");
            return;
        }
        showProgress();
        // Start authenticating...
        mValidThread =
            Net.attemptValidate(userName, password, phoneNumber, validationCode, mHandler,
                ValidationActivity.this);
        
	}
	

    @Override
    protected Dialog onCreateDialog(int id) {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(getText(R.string.ui_activity_authenticating));
        dialog.setIndeterminate(true);
        dialog.setCancelable(true);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                Log.i(TAG, "dialog cancel has been invoked");
                if (mValidThread != null) {
                	mValidThread.interrupt();
                    finish();
                }
            }
        });
        return dialog;
    }

	public void handleGetNewValidation(View view) {
		Log.i(TAG, "Handle get new validation");
        showProgress();
        // Start authenticating...
        mValidThread =
            Net.attemptNewValidateCode(userName, password, phoneNumber, mHandler,
                ValidationActivity.this);
		
	}
    /**
     * Shows the progress UI for a lengthy operation.
     */
    protected void showProgress() {
        showDialog(0);
    }

    protected void hideProgress() {
        dismissDialog(0);
    }

    public void onValidationResult(boolean result, String message) {
        Log.i(TAG, "onValidationResult(" + result + ")");
        // Hide the progress dialog
        hideProgress();
        messageText.setText(message);
        if(result) {
        	Toast.makeText(this, "Validation of your account was successful", Toast.LENGTH_LONG).show();
            final Runnable runnable = new Runnable() {
                public void run() {
                	try {
                		SyncAdapter.doSync();
					} catch (Exception e) {
						e.printStackTrace();
					}
                }
            };
            Net.performOnBackgroundThread(runnable);				
        	finish();
        }
    }
    
    public void onNewValidationCodeResult(boolean result, String message) {
        Log.i(TAG, "onNewValidationCodeResult(" + result + ")");
        // Hide the progress dialog
        hideProgress();
        newValidationCodeMessage.setText(message);
        if(result) {
        	// messageText.
        }
    }
    
}
