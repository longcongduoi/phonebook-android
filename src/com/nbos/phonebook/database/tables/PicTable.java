package com.nbos.phonebook.database.tables;

/**
 * This interface represents the columns and SQLite statements for the PicTable.
 * This table is represented in the sqlite database as Pic column.
 * 				  
 * Generated Class. Do not modify!
 * 
 * @author MDSDACP Team - goetzfred@fh-bingen.de 
 * @date 2011.07.19
 */
public interface PicTable {
	String TABLE_NAME = "pic";

	String ID = "_id";
	String SERVERID = "serverid";
	String PICID = "picid";
	String ACCOUNT = "account";

	String[] ALL_COLUMNS = new String[]{ID, SERVERID, PICID, ACCOUNT};

	String SQL_CREATE = "CREATE TABLE " + TABLE_NAME + " ( " + ID
			+ " INTEGER PRIMARY KEY AUTOINCREMENT " + "," + SERVERID + " TEXT"
			+ "," + PICID + " TEXT" + "," + ACCOUNT + " TEXT" + " )";

	String SQL_INSERT = "INSERT INTO " + TABLE_NAME + " (" + SERVERID + ","
			+ PICID + "," + ACCOUNT + ") VALUES ( ?, ?, ? )";

	String SQL_DROP = "DROP TABLE IF EXISTS " + TABLE_NAME;

	String WHERE_ID_EQUALS = ID + "=?";
}
