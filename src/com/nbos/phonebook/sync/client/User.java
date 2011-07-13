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

import android.util.Log;

import org.json.JSONObject;

/**
 * Represents a sample SyncAdapter user
 */
public class User {

    private String mUserName = null;
    private String mFirstName = null;
    private String mLastName = null;
    private String mCellPhone = null;
    private String mOfficePhone = null;
    private String mHomePhone = null;
    private String mEmail = null;
    private boolean mDeleted;
    private String mUserId, mContactId, mPicId;
    static String tag = "User";

    public String getUserId() {
        return mUserId;
    }

    public String getUserName() {
        return mUserName;
    }

    public String getFirstName() {
        return mFirstName;
    }

    public String getLastName() {
        return mLastName;
    }

    public String getCellPhone() {
        return mCellPhone;
    }

    public String getOfficePhone() {
        return mOfficePhone;
    }

    public String getHomePhone() {
        return mHomePhone;
    }

    public String getEmail() {
        return mEmail;
    }

    public boolean isDeleted() {
        return mDeleted;
    }
    
    public String getContactId() {
    	return mContactId;
    }

    
  /*  public User(String name, String firstName, String lastName,
        String cellPhone, String officePhone, String homePhone, String email,
        Boolean deleted, Integer userId) {
        mUserName = name;
        mFirstName = firstName;
        mLastName = lastName;
        mCellPhone = cellPhone;
        mOfficePhone = officePhone;
        mHomePhone = homePhone;
        mEmail = email;
        mDeleted = deleted;
        mUserId = userId;
    }*/

    public String getPicId() {
		return mPicId;
	}

    public void setPicId(String picId) {
		mPicId = picId;
	}

	public User(String name, String number, String userId) {
            mUserName = name;
            mFirstName = name;
            mCellPhone = number;
            mContactId = "0";
            mDeleted = false;
            mUserId = userId;

    }

    public User(String name, String number, String userId, String contactId) {
        mUserName = name;
        mFirstName = name;
        mCellPhone = number;
        mDeleted = false;
        mUserId = userId;
        mContactId = contactId;
        mLastName = null;
        // mCellPhone = cellPhone;
        mOfficePhone = null;
        mHomePhone = null;
        mEmail = null;
        
    }

    /**
     * Creates and returns an instance of the user from the provided JSON data.
     * 
     * @param user The JSONObject containing user data
     * @return user The new instance of Voiper user created from the JSON data.
     */
    public static User valueOf(JSONObject user) {
        try {
            // final String userName = user.getString("u");
            final String firstName = user.has("name") ? user.getString("name") : null;
            // final String lastName = user.has("l") ? user.getString("l") : null;
            final String cellPhone = user.has("number") ? user.getString("number") : null;
            //final String officePhone =
              //  user.has("o") ? user.getString("o") : null;
            //final String homePhone = user.has("h") ? user.getString("h") : null;
            //final String email = user.has("e") ? user.getString("e") : null;
            //final boolean deleted =
                // user.has("d") ? user.getBoolean("d") : false;
            final int userId = user.getInt("id");
            String picId = user.getString("pic");
            Log.i(tag, "name: "+firstName+", picId is: "+picId);
            User u = new User(firstName, cellPhone, new Integer(userId).toString()); 
            u.setPicId(picId);
            return u;
        } catch (final Exception ex) {
            Log.i(tag, "Error parsing JSON user object" + ex.toString());

        }
        return null;

    }

/*    public static User valueOf(JSONObject user) {
        try {
            final String userName = user.getString("u");
            final String firstName = user.has("f") ? user.getString("f") : null;
            final String lastName = user.has("l") ? user.getString("l") : null;
            final String cellPhone = user.has("m") ? user.getString("m") : null;
            final String officePhone =
                user.has("o") ? user.getString("o") : null;
            final String homePhone = user.has("h") ? user.getString("h") : null;
            final String email = user.has("e") ? user.getString("e") : null;
            final boolean deleted =
                user.has("d") ? user.getBoolean("d") : false;
            final int userId = user.getInt("i");
            return new User(userName, firstName, lastName, cellPhone,
                officePhone, homePhone, email, deleted, userId);
        } catch (final Exception ex) {
            Log.i("User", "Error parsing JSON user object" + ex.toString());

        }
        return null;

    }
    */

    /**
     * Represents the User's status messages
     * 
     */
    public static class Status {
        private final Integer mUserId;
        private final String mStatus;

        public int getUserId() {
            return mUserId;
        }

        public String getStatus() {
            return mStatus;
        }

        public Status(Integer userId, String status) {
            mUserId = userId;
            mStatus = status;
        }

        public static User.Status valueOf(JSONObject userStatus) {
            try {
                final int userId = userStatus.getInt("i");
                final String status = userStatus.getString("s");
                return new User.Status(userId, status);
            } catch (final Exception ex) {
                Log.i("User.Status", "Error parsing JSON user object");
            }
            return null;
        }
    }

}
