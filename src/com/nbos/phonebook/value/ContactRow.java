package com.nbos.phonebook.value;


public class ContactRow implements Comparable<ContactRow> {
	public String id;
	public String name;
	
	public ContactRow(String id, String name) {
		this.id = id;
		this.name = name;
	}
	@Override
	public int compareTo(ContactRow other) {
		return this.name.compareTo(other.name);
	}
}