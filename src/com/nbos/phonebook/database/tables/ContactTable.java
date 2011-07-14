package com.nbos.phonebook.database.tables;

/**
 * This interface represents the columns and SQLite statements for the ContactTable.
 * This table is represented in the sqlite database as Contact column.
 * 				  
 * Generated Class. Do not modify!
 * 
 * @author MDSDACP Team - goetzfred@fh-bingen.de 
 * @date 2011.07.14
 */
public interface ContactTable {
	String TABLE_NAME = "contact";

	String ID = "_id";
	String CONTACTID = "contactid";
	String SERVERID = "serverid";
	String PICID = "picid";

	String[] ALL_COLUMNS = new String[]{ID, CONTACTID, SERVERID, PICID};

	String SQL_CREATE = "CREATE TABLE " + TABLE_NAME + " ( " + ID
			+ " INTEGER PRIMARY KEY AUTOINCREMENT " + "," + CONTACTID + " TEXT"
			+ "," + SERVERID + " TEXT" + "," + PICID + " TEXT" + " )";

	String SQL_INSERT = "INSERT INTO " + TABLE_NAME + " (" + CONTACTID + ","
			+ SERVERID + "," + PICID + ") VALUES ( ?, ?, ? )";

	String SQL_DROP = "DROP TABLE IF EXISTS " + TABLE_NAME;

	String WHERE_ID_EQUALS = ID + "=?";
}
