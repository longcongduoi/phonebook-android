package nbos.android.content;

import android.content.ContentValues;
import android.database.Cursor;

public class DbUtils {
    public static void cursorStringToContentValuesIfPresent(Cursor cursor, ContentValues values,
            String column) {
        final int index = cursor.getColumnIndexOrThrow(column);
        if (!cursor.isNull(index)) {
            values.put(column, cursor.getString(index));
        }
    }

    /**
     * Reads a Long out of a column in a Cursor and writes it to a ContentValues.
     * Adds nothing to the ContentValues if the column isn't present or if its value is null.
     *
     * @param cursor The cursor to read from
     * @param column The column to read
     * @param values The {@link ContentValues} to put the value into
     */
    public static void cursorLongToContentValuesIfPresent(Cursor cursor, ContentValues values,
            String column) {
        final int index = cursor.getColumnIndexOrThrow(column);
        if (!cursor.isNull(index)) {
            values.put(column, cursor.getLong(index));
        }
    }

}
