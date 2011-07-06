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

import com.nbos.phonebook.DatabaseHelper;
import com.nbos.phonebook.Widget;
import com.nbos.phonebook.Widget.AppService;
import com.nbos.phonebook.sync.Constants;
import com.nbos.phonebook.sync.client.Group;
import com.nbos.phonebook.sync.client.NetworkUtilities;
import com.nbos.phonebook.sync.client.User;
import com.nbos.phonebook.sync.client.User.Status;
import com.nbos.phonebook.sync.platform.ContactManager;

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
        List<User> users;
        List<Group> groups;
        List<Group> sharedBooks;
        List<Status> statuses;
        String authtoken = null;
        
        // ContactManager.setDirtyContacts(mContext); // for testing
        Cursor dataCursor = null,
        	rawContactsCursor = null;
        
        try {
             // use the account manager to request the credentials
        	String phoneNumber = mAccountManager.getUserData(account, Constants.PHONE_NUMBER_KEY);
        	Log.i(TAG, "phone number is: "+phoneNumber);
             authtoken =
                mAccountManager.blockingGetAuthToken(account,
                    Constants.AUTHTOKEN_TYPE, true /* notifyAuthFailure */);
             // fetch updates from the sample service over the cloud
             boolean valid = NetworkUtilities.checkValidAccount(account, authtoken, 
            		 mAccountManager.getUserData(account, Constants.PHONE_NUMBER_KEY));
             // start the confirmation activity if not valid
             
             Log.i(TAG, "valid account is: "+valid);
             List<User> contacts = DatabaseHelper.getContacts(false, mContext);
             dataCursor = DatabaseHelper.getData(mContext);
             Object[] update = NetworkUtilities.fetchFriendUpdates(account, authtoken,
                    mLastUpdated);
             users =  (List<User>) update[0];
             groups = (List<Group>) update[1];
             sharedBooks = (List<Group>) update[2];
             ContactManager.syncContacts(mContext, account.name, users, contacts, dataCursor);
             contacts = DatabaseHelper.getContacts(false, mContext);
             dataCursor = DatabaseHelper.getData(mContext);
             rawContactsCursor = DatabaseHelper.getRawContactsCursor(mContext.getContentResolver(), false);
             ContactManager.syncGroups(mContext, account.name, groups, contacts, dataCursor, rawContactsCursor);
             ContactManager.syncSharedBooks(mContext, account.name, sharedBooks, contacts, dataCursor, rawContactsCursor);
             
             NetworkUtilities.sendFriendUpdates(account, authtoken,
                     mLastUpdated, true, mContext);
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
        	dataCursor.close();
        }
    }
}
