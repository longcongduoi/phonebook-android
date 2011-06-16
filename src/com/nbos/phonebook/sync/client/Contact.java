package com.nbos.phonebook.sync.client;

public class Contact {
	String id, serverId;
	String name, number;
	public Contact(String id, String serverId, String number, String name) {
		super();
		this.id = id;
		this.serverId = serverId;
		this.number = number;
		this.name = name;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getServerId() {
		return serverId;
	}
	public String getNumber() {
		return number;
	}
	public void setNumber(String number) {
		this.number = number;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}
