package com.nbos.phonebook.sync.client;

public class SharingBook {
	public String groupId, contactId;
	public boolean deleted = false;
	public SharingBook(String groupId, String contactId, boolean deleted) {
		this.groupId = groupId;
		this.contactId = contactId;
		this.deleted = deleted;
	}
}
