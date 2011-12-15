package com.nbos.phonebook.value;

public class PicData {
	public String picId;//, picHash;
	public Long picSize; 
	public PicData(long picId, long picSize) {//, String picHash) {
		this.picId = new Long(picId).toString();
		this.picSize = picSize;
		// this.picHash =  picHash;
	}
	
	public String toString() {
		return "Pic data - picId: "+picId+", size: "+picSize;
	}
}
