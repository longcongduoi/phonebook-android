package com.nbos.phonebook.sync.client;

public class SharingBook {
	int groupId, contactId;
	public SharingBook(int groupId, int contactId) {
		super();
		this.groupId = groupId;
		this.contactId = contactId;
	}

	public int getGroupId() {
		return groupId;
	}

	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

	public int getContactId() {
		return contactId;
	}

	public void setContactId(int contactId) {
		this.contactId = contactId;
	}
}
