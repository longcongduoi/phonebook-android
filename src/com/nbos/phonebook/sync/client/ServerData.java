package com.nbos.phonebook.sync.client;

import android.util.Log;

public class ServerData {
	static String tag = "ServerData";
	public String contactId, serverId, picId, picSize, picHash;
	public ServerData(String contactId, String serverId, String picId, String picSize, String picHash) {
		Log.i(tag, "contactId: "+contactId+", serverId: "+serverId+", picId: "+picId+", size: "+picSize+", picHash: "+picHash);
		this.contactId = contactId;
		this.serverId = serverId;
		this.picId = picId;
		this.picSize = picSize;
		this.picHash = picHash;
	}
	
	public String toString() {
		return "contactId: "+contactId+", serverId: "+serverId+", picId: "+picId+", size: "+picSize+", picHash: "+picHash;
	}
	
}
