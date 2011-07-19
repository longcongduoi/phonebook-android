package com.nbos.phonebook.sync.client;


public class PhoneContact extends Contact {

	public String contactId, rawContactId;
	public PhoneContact(String name, String number, String serverId, String contactId, String rawContactId) {
		super(name, number, serverId);
		this.contactId = contactId;
		this.rawContactId = rawContactId;
	}

	public PhoneContact(String name, String number, String serverId) {
		super(name, number, serverId);
	}
}
