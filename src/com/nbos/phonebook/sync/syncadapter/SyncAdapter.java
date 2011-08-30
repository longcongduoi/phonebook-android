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

package com.nbos.phonebook.sync.syncadapter;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.json.JSONException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import com.nbos.phonebook.ValidationActivity;
import com.nbos.phonebook.Widget;
import com.nbos.phonebook.Widget.AppService;
import com.nbos.phonebook.sync.Constants;
import com.nbos.phonebook.sync.client.Net;
import com.nbos.phonebook.sync.platform.Cloud;

/**
 * SyncAdapter implementation for syncing sample SyncAdapter contacts to the
 * platform ContactOperations provider.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String tag = "SyncAdapter";

    private static AccountManager accountManager;
    private static Context context;
    static Account account;
    static String authtoken;
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        SyncAdapter.context = context;
        accountManager = AccountManager.get(context);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
        ContentProviderClient provider, SyncResult syncResult) {
    	SyncAdapter.account = account;
        try {
             // use the account manager to request the credentials
        	String phoneNumber = accountManager.getUserData(account, Constants.PHONE_NUMBER_KEY);
        	Log.i(tag, "phone number is: "+phoneNumber);
             authtoken = accountManager.blockingGetAuthToken(account, Constants.AUTHTOKEN_TYPE, true /* notifyAuthFailure */);
             // fetch updates from the sample service over the cloud
             boolean valid = Net.checkValidAccount(account, authtoken, 
            		 accountManager.getUserData(account, Constants.PHONE_NUMBER_KEY));
             // start the confirmation activity if not valid
             
             Log.i(tag, "valid account is: "+valid);
             if(!valid) 
             {
                 final Intent intent = new Intent(context, ValidationActivity.class);
                 intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                 intent.putExtra(Net.PARAM_USERNAME, account.name);
                 intent.putExtra(Net.PARAM_PASSWORD, authtoken);
                 intent.putExtra(Net.PARAM_PHONE_NUMBER, phoneNumber);
                 context.startActivity(intent);
                 return;
             }
             doSync();
        } catch (Exception e) {
			e.printStackTrace();
		}
    }

	public static void doSync() throws AuthenticationException, ParseException, JSONException, IOException {
		Log.i(tag, "doSync()");
        String lastUpdated = accountManager.getUserData(account, Constants.ACCOUNT_LAST_UPDATED),
        	lastUpdateStarted = accountManager.getUserData(account, Constants.ACCOUNT_LAST_UPDATE_STARTED);
        Log.i(tag, "Last update started: "+lastUpdateStarted+", updated is: "+lastUpdated);
        Cloud cloud = new Cloud(context, account.name, authtoken);
        String startTimestamp = cloud.getTimestamp();
        /*if(lastUpdateStarted != null) 
        {
        	if(lastUpdated == null 
        	||(lastUpdated != null 
        	   && (Long.parseLong(lastUpdated) < Long.parseLong(lastUpdateStarted))))
        	{
        		Log.i(tag, "Previous sync has not finished - returning");
        		return;
        	}
        }*/
        accountManager.setUserData(account, Constants.ACCOUNT_LAST_UPDATE_STARTED, startTimestamp);
        String endTimestamp = cloud.sync(lastUpdated);
        Log.i(tag, "Timestamp is: "+endTimestamp);
        accountManager.setUserData(account, Constants.ACCOUNT_LAST_UPDATED, endTimestamp);
        Widget.AppService.message = "Phonebook last updated: "+DateFormat.getInstance().format(new Date(Long.parseLong(endTimestamp)));
        context.startService(new Intent(context, AppService.class));
	}
}
