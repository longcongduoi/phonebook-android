package com.nbos.phonebook.model;

/**
 * Generated model class for usage in your application, defined by classifiers in ecore diagram
 * 		
 * Generated Class. Do not modify!
 * 
 * @author MDSDACP Team - goetzfred@fh-bingen.de 
 * @date 2011.05.31	 
 */
public class Book {
	private Long id;
	private java.lang.String bookid;
	private java.lang.String contactid;
	private boolean dirty;
	private long serverid;

	/**
	 * Set id
	 *
	 * @param id from type java.lang.Long
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Get id
	 *
	 * @return id from type java.lang.Long				
	 */
	public Long getId() {
		return this.id;
	}

	/**
	 * Set bookid
	 *
	 * @param bookid from type java.lang.String
	 */
	public void setBookId(java.lang.String bookid) {
		this.bookid = bookid;
	}

	/**
	 * Get bookid
	 *
	 * @return bookid from type java.lang.String				
	 */
	public java.lang.String getBookId() {
		return this.bookid;
	}

	/**
	 * Set contactid
	 *
	 * @param contactid from type java.lang.String
	 */
	public void setContactId(java.lang.String contactid) {
		this.contactid = contactid;
	}

	/**
	 * Get contactid
	 *
	 * @return contactid from type java.lang.String				
	 */
	public java.lang.String getContactId() {
		return this.contactid;
	}

	/**
	 * Set dirty
	 *
	 * @param dirty from type boolean
	 */
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	/**
	 * Get dirty
	 *
	 * @return dirty from type boolean				
	 */
	public boolean getDirty() {
		return this.dirty;
	}

	/**
	 * Set serverid
	 *
	 * @param serverid from type long
	 */
	public void setServerId(long serverid) {
		this.serverid = serverid;
	}

	/**
	 * Get serverid
	 *
	 * @return serverid from type long				
	 */
	public long getServerId() {
		return this.serverid;
	}

}
