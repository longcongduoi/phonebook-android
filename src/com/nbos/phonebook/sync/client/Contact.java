package com.nbos.phonebook.sync.client;

public class Contact {
	int id;
	long number;
	String name;
	public Contact(int id, long number, String name) {
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
	public long getNumber() {
		return number;
	}
	public void setNumber(long number) {
		this.number = number;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
}
