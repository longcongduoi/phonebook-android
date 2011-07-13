package com.nbos.phonebook.sync.client;

public class PhoneContact extends Contact {

	public String contactId;
	public PhoneContact(String name, String number, String serverId, String contactId) {
		super(name, number, serverId);
		this.contactId = contactId;
	}

	public PhoneContact(String name, String number, String serverId) {
		super(name, number, serverId);
	}
}
