package nbos.android.content;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.RawContacts.Data;

public class RawContact {
    public static EntityIterator newEntityIterator(Cursor cursor) {
        return new EntityIteratorImpl(cursor);
    }

    private static class EntityIteratorImpl extends CursorEntityIterator {
        private static final String[] DATA_KEYS = new String[]{
                Data.DATA1,
                Data.DATA2,
                Data.DATA3,
                Data.DATA4,
                Data.DATA5,
                Data.DATA6,
                Data.DATA7,
                Data.DATA8,
                Data.DATA9,
                Data.DATA10,
                Data.DATA11,
                Data.DATA12,
                Data.DATA13,
                Data.DATA14,
                Data.DATA15,
                Data.SYNC1,
                Data.SYNC2,
                Data.SYNC3,
                Data.SYNC4};

        public EntityIteratorImpl(Cursor cursor) {
            super(cursor);
        }

        @Override
        public Entity getEntityAndIncrementCursor(Cursor cursor)
                throws RemoteException {
            final int columnRawContactId = cursor.getColumnIndexOrThrow(RawContacts._ID);
            final long rawContactId = cursor.getLong(columnRawContactId);

            // we expect the cursor is already at the row we need to read from
            ContentValues cv = new ContentValues();
            DbUtils.cursorStringToContentValuesIfPresent(cursor, cv, RawContacts.ACCOUNT_NAME);
            DbUtils.cursorStringToContentValuesIfPresent(cursor, cv, RawContacts.ACCOUNT_TYPE);
            DbUtils.cursorLongToContentValuesIfPresent(cursor, cv, RawContacts._ID);
            DbUtils.cursorLongToContentValuesIfPresent(cursor, cv, RawContacts.DIRTY);
            DbUtils.cursorLongToContentValuesIfPresent(cursor, cv, RawContacts.VERSION);
            DbUtils.cursorStringToContentValuesIfPresent(cursor, cv, RawContacts.SOURCE_ID);
            DbUtils.cursorStringToContentValuesIfPresent(cursor, cv, RawContacts.SYNC1);
            DbUtils.cursorStringToContentValuesIfPresent(cursor, cv, RawContacts.SYNC2);
            DbUtils.cursorStringToContentValuesIfPresent(cursor, cv, RawContacts.SYNC3);
            DbUtils.cursorStringToContentValuesIfPresent(cursor, cv, RawContacts.SYNC4);
            DbUtils.cursorLongToContentValuesIfPresent(cursor, cv, RawContacts.DELETED);
            DbUtils.cursorLongToContentValuesIfPresent(cursor, cv, RawContacts.CONTACT_ID);
            DbUtils.cursorLongToContentValuesIfPresent(cursor, cv, RawContacts.STARRED);
            //DatabaseUtils.cursorIntToContentValuesIfPresent(cursor, cv, RawContacts.IS_RESTRICTED);
            // DatabaseUtils.cursorIntToContentValuesIfPresent(cursor, cv, RawContacts.NAME_VERIFIED);
            Entity contact = new Entity(cv);

            // read data rows until the contact id changes
            do {
                if (rawContactId != cursor.getLong(columnRawContactId)) {
                    break;
                }
                // add the data to to the contact
                cv = new ContentValues();
                cv.put(Data._ID, cursor.getLong(cursor.getColumnIndexOrThrow("data_id")));//Entity.DATA_ID)));
                // DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, cv,
                   //     Data.RES_PACKAGE);
                DbUtils.cursorStringToContentValuesIfPresent(cursor, cv, Data.MIMETYPE);
                DbUtils.cursorLongToContentValuesIfPresent(cursor, cv, Data.IS_PRIMARY);
                DbUtils.cursorLongToContentValuesIfPresent(cursor, cv,
                        Data.IS_SUPER_PRIMARY);
                DbUtils.cursorLongToContentValuesIfPresent(cursor, cv, Data.DATA_VERSION);
                DbUtils.cursorStringToContentValuesIfPresent(cursor, cv,
                        CommonDataKinds.GroupMembership.GROUP_SOURCE_ID);
                DbUtils.cursorStringToContentValuesIfPresent(cursor, cv,
                        Data.DATA_VERSION);
                for (String key : DATA_KEYS) {
                    final int columnIndex = cursor.getColumnIndexOrThrow(key);
                    if (cursor.isNull(columnIndex)) {
                        // don't put anything
                    } else {
                        try {
                            cv.put(key, cursor.getString(columnIndex));
                        } catch (SQLiteException e) {
                            cv.put(key, cursor.getBlob(columnIndex));
                        }
                    }
                    // TODO: go back to this version of the code when bug
                    // http://b/issue?id=2306370 is fixed.
//                    if (cursor.isNull(columnIndex)) {
//                        // don't put anything
//                    } else if (cursor.isLong(columnIndex)) {
//                        values.put(key, cursor.getLong(columnIndex));
//                    } else if (cursor.isFloat(columnIndex)) {
//                        values.put(key, cursor.getFloat(columnIndex));
//                    } else if (cursor.isString(columnIndex)) {
//                        values.put(key, cursor.getString(columnIndex));
//                    } else if (cursor.isBlob(columnIndex)) {
//                        values.put(key, cursor.getBlob(columnIndex));
//                    }
                }
                contact.addSubValue(ContactsContract.Data.CONTENT_URI, cv);
            } while (cursor.moveToNext());

            return contact;
        }

    }
}