package com.nbos.phonebook.database.tables;

/**
 * This interface represents the columns and SQLite statements for the ContactTable.
 * This table is represented in the sqlite database as Contact column.
 * 				  
 * Generated Class. Do not modify!
 * 
 * @author MDSDACP Team - goetzfred@fh-bingen.de 
 * @date 2011.11.08
 */
public interface ContactTable {
	String TABLE_NAME = "contact";

	String ID = "_id";
	String CONTACTID = "contactid";
	String RAWCONTACTID = "rawcontactid";

	String[] ALL_COLUMNS = new String[]{ID, CONTACTID, RAWCONTACTID};

	String SQL_CREATE = "CREATE TABLE " + TABLE_NAME + " ( " + ID
			+ " INTEGER PRIMARY KEY AUTOINCREMENT " + "," + CONTACTID
			+ " INTEGER" + "," + RAWCONTACTID + " INTEGER" + " )";

	String SQL_INSERT = "INSERT INTO " + TABLE_NAME + " (" + CONTACTID + ","
			+ RAWCONTACTID + ") VALUES ( ?, ? )";

	String SQL_DROP = "DROP TABLE IF EXISTS " + TABLE_NAME;

	String WHERE_ID_EQUALS = ID + "=?";
}
