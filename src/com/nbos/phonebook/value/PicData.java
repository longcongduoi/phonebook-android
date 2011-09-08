package com.nbos.phonebook.value;

public class PicData {
	public String serverId, picId, picHash;
	public Long picSize; 
	public PicData(long serverId, long picId, long picSize, String picHash) {
		this.serverId = new Long(serverId).toString();
		this.picId = new Long(picId).toString();
		this.picSize = picSize;
		this.picHash =  picHash;
	}
	
}
