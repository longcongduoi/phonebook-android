package com.nbos.phonebook.value;

public class ContactRow implements Comparable<ContactRow> {
	public String id, rawContactId, name;
	public ContactRow(String id, String name) {
		this.id = id;
		this.name = name;
	}
	
	public ContactRow(String id, String rawContactId, String name) {
		this.id = id;
		this.rawContactId = rawContactId;
		this.name = name;
	}

	public int compareTo(ContactRow other) {
		return this.name.compareTo(other.name);
	}
}