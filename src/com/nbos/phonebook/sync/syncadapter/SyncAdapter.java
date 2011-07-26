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
import java.util.List;

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
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

import com.nbos.phonebook.Db;
import com.nbos.phonebook.ValidationActivity;
import com.nbos.phonebook.Widget;
import com.nbos.phonebook.Widget.AppService;
import com.nbos.phonebook.sync.Constants;
import com.nbos.phonebook.sync.client.Contact;
import com.nbos.phonebook.sync.client.Group;
import com.nbos.phonebook.sync.client.Net;
import com.nbos.phonebook.sync.client.PhoneContact;
import com.nbos.phonebook.sync.client.User.Status;
import com.nbos.phonebook.sync.platform.SyncManager;

/**
 * SyncAdapter implementation for syncing sample SyncAdapter contacts to the
 * platform ContactOperations provider.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = "SyncAdapter";

    private final AccountManager mAccountManager;
    private final Context mContext;

    private Date mLastUpdated;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContext = context;
        mAccountManager = AccountManager.get(context);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
        ContentProviderClient provider, SyncResult syncResult) {
        String authtoken = null;
        SyncManager syncManager = null;
        // ContactManager.setDirtyContacts(mContext); // for testing
        // Cursor dataCursor = null,
        	// rawContactsCursor = null;
        
        try {
             // use the account manager to request the credentials
        	String phoneNumber = mAccountManager.getUserData(account, Constants.PHONE_NUMBER_KEY);
        	Log.i(TAG, "phone number is: "+phoneNumber);
             authtoken =
                mAccountManager.blockingGetAuthToken(account,
                    Constants.AUTHTOKEN_TYPE, true /* notifyAuthFailure */);
             // fetch updates from the sample service over the cloud
             boolean valid = Net.checkValidAccount(account, authtoken, 
            		 mAccountManager.getUserData(account, Constants.PHONE_NUMBER_KEY));
             // start the confirmation activity if not valid
             
             Log.i(TAG, "valid account is: "+valid);
             if(!valid) 
             {
                 final Intent intent = new Intent(mContext, ValidationActivity.class);
                 intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                 intent.putExtra(Net.PARAM_USERNAME, account.name);
                 intent.putExtra(Net.PARAM_PASSWORD, authtoken);
                 intent.putExtra(Net.PARAM_PHONE_NUMBER, phoneNumber);
                 mContext.startActivity(intent);
                 
                 // intent.putExtra(AuthenticatorActivity.PARAM_AUTHTOKEN_TYPE, authTokenType);
                 // intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
                 return;

             }
             Object[] update = Net.fetchFriendUpdates(account, authtoken, mLastUpdated);
             syncManager = new SyncManager(mContext, account.name, update);
             
             Net.sendFriendUpdates(account, authtoken, mLastUpdated, true, mContext);
             mLastUpdated = new Date();                     
             Widget.AppService.message = "Phonebook last updated: "+DateFormat.getInstance().format(mLastUpdated);
             mContext.startService(new Intent(mContext, AppService.class));
            // update the last synced date.
            
            // update platform contacts.
            // fetch and update status messages for all the synced users.
            // statuses = NetworkUtilities.fetchFriendStatuses(account, authtoken);
            // ContactManager.insertStatuses(mContext, account.name, statuses);

        } catch (final AuthenticatorException e) {
            syncResult.stats.numParseExceptions++;
            Log.e(TAG, "AuthenticatorException", e);
        } catch (final OperationCanceledException e) {
            Log.e(TAG, "OperationCanceledExcetpion", e);
        } catch (final IOException e) {
            Log.e(TAG, "IOException", e);
            syncResult.stats.numIoExceptions++;
        } catch (final AuthenticationException e) {
            mAccountManager.invalidateAuthToken(Constants.ACCOUNT_TYPE,
                authtoken);
            syncResult.stats.numAuthExceptions++;
            Log.e(TAG, "AuthenticationException", e);
        } catch (final ParseException e) {
            syncResult.stats.numParseExceptions++;
            Log.e(TAG, "ParseException", e);
        } catch (final JSONException e) {
            syncResult.stats.numParseExceptions++;
            Log.e(TAG, "JSONException", e);
        }
        finally {
        	if(syncManager != null)
        		syncManager.close();
        }
    }
}
