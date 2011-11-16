package com.nbos.phonebook.contentprovider;

import com.nbos.phonebook.database.Database;

import com.nbos.phonebook.database.tables.*;

import android.provider.BaseColumns;
import android.text.TextUtils;
import android.content.ContentUris;
import android.database.sqlite.SQLiteQueryBuilder;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

/**
 * Content provider implementation
 * The authority of the content provider is: content://de.mdsdacp.provider.defaultname
 * 
 * More information about content providers:
 * @see <a href="http://developer.android.com/reference/android/content/ContentProvider.html">Reference</a>
 * @see <a href="http://developer.android.com/guide/topics/providers/content-providers.html">Tutorial</a>
 * @see <a href="http://developer.android.com/guide/topics/testing/contentprovider_testing.html">Content Provider Testing</a>
 *
 * Generated Class. Do not modify!
 * 
 * @author MDSDACP Team - goetzfred@fh-bingen.de 
 * @date 2011.05.24
 */
public class Provider extends ContentProvider {
	private static final String TAG = "com.nbos.phonebook.contentprovider.Provider";

	public static final String AUTHORITY = "com.nbos.phonebook.provider.defaultname";
	public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

	public static final Uri BOOK_CONTENT_URI = Uri.withAppendedPath(
			Provider.AUTHORITY_URI, BookContent.CONTENT_PATH);

	public static final Uri CONTACT_CONTENT_URI = Uri.withAppendedPath(
			Provider.AUTHORITY_URI, ContactContent.CONTENT_PATH);

	private static final UriMatcher URI_MATCHER;

	private Database db = null;

	private static final int BOOK_DIR = 0;
	private static final int BOOK_ID = 1;
	private static final int CONTACT_DIR = 2;
	private static final int CONTACT_ID = 3;

	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(AUTHORITY, BookContent.CONTENT_PATH, BOOK_DIR);
		URI_MATCHER.addURI(AUTHORITY, BookContent.CONTENT_PATH + "/#", BOOK_ID);
		URI_MATCHER.addURI(AUTHORITY, ContactContent.CONTENT_PATH, CONTACT_DIR);
		URI_MATCHER.addURI(AUTHORITY, ContactContent.CONTENT_PATH + "/#",
				CONTACT_ID);
	}

	/**
	 * Provides the content information of the BookTable.
	 * 
	 * CONTENT_PATH: book (String)
	 * CONTENT_TYPE: vnd.android.cursor.dir/vnd.mdsdacp.book (String)
	 * CONTENT_ITEM_TYPE: vnd.android.cursor.item/vnd.mdsdacp.book (String)
	 * ALL_COLUMNS: Provides the same information as BookTable.ALL_COLUMNS (String[])
	 */
	public static final class BookContent implements BaseColumns {
		/**
		 * Specifies the content path of the BookTable for the required uri
		 * Exact URI: content://de.mdsdacp.provider.defaultname/book
		 */
		public static final String CONTENT_PATH = "book";

		/**
		 * Specifies the type for the folder and the single item of the BookTable  
		 */
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.mdsdacp.book";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.mdsdacp.book";

		/**
		 * Contains all columns of the BookTable
		 */
		public static final String[] ALL_COLUMNS = BookTable.ALL_COLUMNS;
	}

	/**
	 * Provides the content information of the ContactTable.
	 * 
	 * CONTENT_PATH: contact (String)
	 * CONTENT_TYPE: vnd.android.cursor.dir/vnd.mdsdacp.contact (String)
	 * CONTENT_ITEM_TYPE: vnd.android.cursor.item/vnd.mdsdacp.contact (String)
	 * ALL_COLUMNS: Provides the same information as ContactTable.ALL_COLUMNS (String[])
	 */
	public static final class ContactContent implements BaseColumns {
		/**
		 * Specifies the content path of the ContactTable for the required uri
		 * Exact URI: content://de.mdsdacp.provider.defaultname/contact
		 */
		public static final String CONTENT_PATH = "contact";

		/**
		 * Specifies the type for the folder and the single item of the ContactTable  
		 */
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.mdsdacp.contact";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.mdsdacp.contact";

		/**
		 * Contains all columns of the ContactTable
		 */
		public static final String[] ALL_COLUMNS = ContactTable.ALL_COLUMNS;
	}

	/**
	 * Instantiate the database, when the content provider is created
	 */
	@Override
	public final boolean onCreate() {
		db = new Database(getContext());
		return true;
	}

	/**
	 * Providing information whether uri returns an item or an directory.
	 * 
	 * @param uri from type Uri
	 * 
	 * @return content_type from type Content.CONTENT_TYPE or Content.CONTENT_ITEM_TYPE
	 *
	 */
	@Override
	public final String getType(final Uri uri) {
		switch (URI_MATCHER.match(uri)) {
			case BOOK_DIR :
				return BookContent.CONTENT_TYPE;
			case BOOK_ID :
				return BookContent.CONTENT_ITEM_TYPE;
			case CONTACT_DIR :
				return ContactContent.CONTENT_TYPE;
			case CONTACT_ID :
				return ContactContent.CONTENT_ITEM_TYPE;
			default :
				throw new IllegalArgumentException("Unsupported URI:" + uri);
		}
	}

	/**
	 * Insert given values to given uri. Uri has to be from type directory (see switch-cases).
	 * Returns uri of inserted element.
	 *
	 * @param uri from type Uri
	 * @param values from type ContentValues
	 *
	 * @return uri of inserted element from type Uri
	 */
	@Override
	public final Uri insert(final Uri uri, final ContentValues values) {
		final SQLiteDatabase dbConnection = db.getWritableDatabase();
		dbConnection.beginTransaction();
		try {
			switch (URI_MATCHER.match(uri)) {
				case BOOK_DIR :
				case BOOK_ID :
					final long bookid = dbConnection.insertOrThrow(
							BookTable.TABLE_NAME, null, values);
					final Uri newBook = ContentUris.withAppendedId(
							BOOK_CONTENT_URI, bookid);
					getContext().getContentResolver().notifyChange(newBook,
							null);
					dbConnection.setTransactionSuccessful();
					return newBook;
				case CONTACT_DIR :
				case CONTACT_ID :
					final long contactid = dbConnection.insertOrThrow(
							ContactTable.TABLE_NAME, null, values);
					final Uri newContact = ContentUris.withAppendedId(
							CONTACT_CONTENT_URI, contactid);
					getContext().getContentResolver().notifyChange(newContact,
							null);
					dbConnection.setTransactionSuccessful();
					return newContact;
				default :
					throw new IllegalArgumentException("Unsupported URI:" + uri);
			}
		} catch (Exception e) {
			Log.e(TAG, "Insert Exception", e);
		} finally {
			dbConnection.endTransaction();
		}
		return null;
	}

	/**
	 * Updates given values of given uri, returning number of affected rows.
	 *
	 * @param uri from type Uri
	 * @param values from type ContentValues
	 * @param selection from type String
	 * @param selectionArgs from type String[]
	 *
	 * @return number of affected rows from type int
	 */
	@Override
	public final int update(final Uri uri, final ContentValues values,
			final String selection, final String[] selectionArgs) {

		final SQLiteDatabase dbConnection = db.getWritableDatabase();
		int count = 0;
		switch (URI_MATCHER.match(uri)) {

			case BOOK_DIR :
				count = dbConnection.update(BookTable.TABLE_NAME, values,
						selection, selectionArgs);
				break;
			case BOOK_ID :
				Long bookId = ContentUris.parseId(uri);
				count = dbConnection.update(
						BookTable.TABLE_NAME,
						values,
						BookTable.ID
								+ "="
								+ bookId
								+ (TextUtils.isEmpty(selection) ? "" : " AND ("
										+ selection + ")"), selectionArgs);
				break;

			case CONTACT_DIR :
				count = dbConnection.update(ContactTable.TABLE_NAME, values,
						selection, selectionArgs);
				break;
			case CONTACT_ID :
				Long contactId = ContentUris.parseId(uri);
				count = dbConnection.update(
						ContactTable.TABLE_NAME,
						values,
						ContactTable.ID
								+ "="
								+ contactId
								+ (TextUtils.isEmpty(selection) ? "" : " AND ("
										+ selection + ")"), selectionArgs);
				break;
			default :
				throw new IllegalArgumentException("Unsupported URI:" + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);

		return count;

	}

	/**
	 * Deletes given elements by their uri (items or directories) and returns number of deleted rows.
	 *
	 * @param uri from type Uri
	 * @param selection from type String
	 * @param selectionArgs from type String[]
	 *
	 * @return number of deleted rows from type int
	 */
	@Override
	public final int delete(final Uri uri, final String selection,
			final String[] selectionArgs) {

		final SQLiteDatabase dbConnection = db.getWritableDatabase();
		int deleteCount = 0;
		try {
			switch (URI_MATCHER.match(uri)) {
				case BOOK_DIR :
					deleteCount = dbConnection.delete(BookTable.TABLE_NAME,
							selection, selectionArgs);
					break;
				case BOOK_ID :
					deleteCount = dbConnection.delete(BookTable.TABLE_NAME,
							BookTable.WHERE_ID_EQUALS, new String[]{uri
									.getPathSegments().get(1)});
					break;
				case CONTACT_DIR :
					deleteCount = dbConnection.delete(ContactTable.TABLE_NAME,
							selection, selectionArgs);
					break;
				case CONTACT_ID :
					deleteCount = dbConnection.delete(ContactTable.TABLE_NAME,
							ContactTable.WHERE_ID_EQUALS, new String[]{uri
									.getPathSegments().get(1)});
					break;

				default :
					throw new IllegalArgumentException("Unsupported URI:" + uri);
			}
		} finally {
			dbConnection.close();
		}

		if (deleteCount > 0) {
			getContext().getContentResolver().notifyChange(uri, null);
		}

		return deleteCount;

	}

	/**
	 * Executes a query on a given uri and returns a Cursor with results.
	 *
	 * @param uri from type Uri
	 * @param projection from type String[]
	 * @param selection from type String 
	 * @param selectionArgs from type String[]
	 * @param sortOrder from type String
	 *
	 * @return cursor with results from type Cursor
	 */
	@Override
	public final Cursor query(final Uri uri, final String[] projection,
			final String selection, final String[] selectionArgs,
			final String sortOrder) {

		final SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		final SQLiteDatabase dbConnection = db.getReadableDatabase();

		switch (URI_MATCHER.match(uri)) {
			case BOOK_ID :
				queryBuilder.appendWhere(BookTable.ID + "="
						+ uri.getPathSegments().get(1));
			case BOOK_DIR :
				queryBuilder.setTables(BookTable.TABLE_NAME);
				break;
			case CONTACT_ID :
				queryBuilder.appendWhere(ContactTable.ID + "="
						+ uri.getPathSegments().get(1));
			case CONTACT_DIR :
				queryBuilder.setTables(ContactTable.TABLE_NAME);
				break;
			default :
				throw new IllegalArgumentException("Unsupported URI:" + uri);
		}

		Cursor cursor = queryBuilder.query(dbConnection, projection, selection,
				selectionArgs, null, null, sortOrder);
		cursor.setNotificationUri(getContext().getContentResolver(), uri);

		return cursor;

	}

}
