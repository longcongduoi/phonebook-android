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

package com.nbos.phonebook.sync;

import com.nbos.phonebook.contentprovider.Provider;

import android.net.Uri;
import android.provider.ContactsContract;

public class Constants {

    /**
     * Account type string.
     */
    public static final String ACCOUNT_TYPE = "com.nbos.phonebook";

    /**
     * Authtoken type string.
     */
    public static final String AUTHTOKEN_TYPE = ACCOUNT_TYPE;
        // "com.example.android.samplesync";

	public static final String
		PHONEBOOK_PROVIDER_URI = "content://" + Provider.AUTHORITY + "/" + Provider.BookContent.CONTENT_PATH,
		// CONTACT_SERVER_ID = ContactsContract.RawContacts.SYNC1,
		PHONE_NUMBER_KEY = "ph",
		ACCOUNT_LAST_UPDATED = "lastUpdated",
		ACCOUNT_LAST_UPDATE_STARTED = "updateStarted";
	public static final Uri 
		SHARE_BOOK_URI = Uri.parse(PHONEBOOK_PROVIDER_URI);
		// PIC_URI = Uri.parse(PHONEBOOK_PROVIDER_URI + "pic");


}
