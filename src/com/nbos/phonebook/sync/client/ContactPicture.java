package com.nbos.phonebook.sync.client;

public class ContactPicture {
	public byte[] pic;
	public String mimeType;
	
	public ContactPicture(byte[] pic, String mimeType) {
		super();
		this.pic = pic;
		this.mimeType = mimeType;
	}
	
}
