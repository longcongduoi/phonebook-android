package com.nbos.phonebook.sync.client;

public class ContactPicture {
	public byte[] pic;
	public String serverId, mimeType;
	
	public ContactPicture(byte[] pic, String serverId, String mimeType) {
		super();
		this.pic = pic;
		this.serverId = serverId;
		this.mimeType = mimeType;
	}
	
}
