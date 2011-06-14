package com.nbos.phonebook.value;


public class ContactRow implements Comparable<ContactRow> {
	public String id;
	public String name;
	public byte[] image;
	public ContactRow(String id, String name, byte[] image) {
		this.id = id;
		this.name = name;
		this.image = image;
	}
	@Override
	public int compareTo(ContactRow other) {
		return this.name.compareTo(other.name);
	}
}