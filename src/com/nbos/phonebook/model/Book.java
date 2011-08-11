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
	private long bookid;
	private long contactid;
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
	 * @param bookid from type long
	 */
	public void setBookId(long bookid) {
		this.bookid = bookid;
	}

	/**
	 * Get bookid
	 *
	 * @return bookid from type long				
	 */
	public long getBookId() {
		return this.bookid;
	}

	/**
	 * Set contactid
	 *
	 * @param contactid from type long
	 */
	public void setContactId(long contactid) {
		this.contactid = contactid;
	}

	/**
	 * Get contactid
	 *
	 * @return contactid from type long				
	 */
	public long getContactId() {
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
