package com.nbos.phonebook.sync.client;

public class Contact {
	int id;
	String name, number;
	public Contact(int id, String number, String name) {
		super();
		this.id = id;
		this.number = number;
		this.name = name;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
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
