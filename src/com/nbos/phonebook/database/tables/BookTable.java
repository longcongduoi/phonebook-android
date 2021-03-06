package com.nbos.phonebook.database.tables;

/**
 * This interface represents the columns and SQLite statements for the BookTable.
 * This table is represented in the sqlite database as Book column.
 * 				  
 * Generated Class. Do not modify!
 * 
 * @author MDSDACP Team - goetzfred@fh-bingen.de 
 * @date 2011.05.31
 */
public interface BookTable {
	String TABLE_NAME = "book";

	String ID = "_id";
	String BOOKID = "bookid";
	String CONTACTID = "contactid";
	String DIRTY = "dirty";
	String DELETED = "deleted";

	String[] ALL_COLUMNS = new String[]{ID, BOOKID, CONTACTID, DIRTY, DELETED};

	String SQL_CREATE = "CREATE TABLE " + TABLE_NAME + " ( " + ID
			+ " INTEGER PRIMARY KEY AUTOINCREMENT " + "," + BOOKID + " INTEGER"
			+ "," + CONTACTID + " INTEGER" + "," + DIRTY + " NUMERIC" + ","
			+ DELETED + " NUMERIC" + " )";

	String SQL_INSERT = "INSERT INTO " + TABLE_NAME + " (" + BOOKID + ","
			+ CONTACTID + "," + DIRTY + "," + DELETED
			+ ") VALUES ( ?, ?, ?, ? )";

	String SQL_DROP = "DROP TABLE IF EXISTS " + TABLE_NAME;

	String WHERE_ID_EQUALS = ID + "=?";
}
