package com.nbos.phonebook.sync.client;

import java.util.List;

public class Group {
	String name;
	int groupId;
	List<Contact> contacts;
	public Group(int groupId, String name, List<Contact> contacts) {
		super();
		this.groupId = groupId;
		this.name = name;
		this.contacts = contacts;
	}
	public int getGroupId() {
		return groupId;
	}
	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<Contact> getContacts() {
		return contacts;
	}
	public void setContacts(List<Contact> contacts) {
		this.contacts = contacts;
	}
}
