package com.nbos.phonebook.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import com.nbos.phonebook.value.PicData;

import android.util.Log;

@SuppressWarnings("all")
public class ImageInfo {
	static String tag = "ImageInfo";
	private int width, height;
	private String mimeType;

	private ImageInfo() {}

	public ImageInfo(File file) throws IOException {
		InputStream is = new FileInputStream(file);
		try {
			processStream(is);
		} finally {
			is.close();
		}
	}

	public ImageInfo(InputStream is) throws IOException {
		processStream(is);
	}

	public ImageInfo(byte[] bytes) throws IOException {
		InputStream is = new ByteArrayInputStream(bytes);
		try {
			processStream(is);
		} finally {
			is.close();
		}
	}

	private void processStream(InputStream is) throws IOException {
		int c1 = is.read();
		int c2 = is.read();
		int c3 = is.read();

		mimeType = null;
		width = height = -1;

		if (c1 == 'G' && c2 == 'I' && c3 == 'F') { // GIF
			is.skip(3);
			width = readInt(is,2,false);
			height = readInt(is,2,false);
			mimeType = "image/gif";
		} else if (c1 == 0xFF && c2 == 0xD8) { // JPG
			while (c3 == 255) {
				int marker = is.read();
				int len = readInt(is,2,true);
				if (marker == 192 || marker == 193 || marker == 194) {
					is.skip(1);
					height = readInt(is,2,true);
					width = readInt(is,2,true);
					mimeType = "image/jpeg";
					break;
				}
				is.skip(len - 2);
				c3 = is.read();
			}
		} else if (c1 == 137 && c2 == 80 && c3 == 78) { // PNG
			is.skip(15);
			width = readInt(is,2,true);
			is.skip(2);
			height = readInt(is,2,true);
			mimeType = "image/png";
		} else if (c1 == 66 && c2 == 77) { // BMP
			is.skip(15);
			width = readInt(is,2,false);
			is.skip(2);
			height = readInt(is,2,false);
			mimeType = "image/bmp";
		} else {
			int c4 = is.read();
			if ((c1 == 'M' && c2 == 'M' && c3 == 0 && c4 == 42)
					|| (c1 == 'I' && c2 == 'I' && c3 == 42 && c4 == 0)) { //TIFF
				boolean bigEndian = c1 == 'M';
				int ifd = 0;
				int entries;
				ifd = readInt(is,4,bigEndian);
				is.skip(ifd - 8);
				entries = readInt(is,2,bigEndian);
				for (int i = 1; i <= entries; i++) {
					int tag = readInt(is,2,bigEndian);
					int fieldType = readInt(is,2,bigEndian);
					long count = readInt(is,4,bigEndian);
					int valOffset;
					if ((fieldType == 3 || fieldType == 8)) {
						valOffset = readInt(is,2,bigEndian);
						is.skip(2);
					} else {
						valOffset = readInt(is,4,bigEndian);
					}
					if (tag == 256) {
						width = valOffset;
					} else if (tag == 257) {
						height = valOffset;
					}
					if (width != -1 && height != -1) {
						mimeType = "image/tiff";
						break;
					}
				}
			}
		}
		if (mimeType == null) {
			throw new IOException("Unsupported image type");
		}
	}
	
	private int readInt(InputStream is, int noOfBytes, boolean bigEndian) throws IOException {
		int ret = 0;
		int sv = bigEndian ? ((noOfBytes - 1) * 8) : 0;
		int cnt = bigEndian ? -8 : 8;
		for(int i=0;i<noOfBytes;i++) {
			ret |= is.read() << sv;
			sv += cnt;
		}
		return ret;
	}

    public static String hash(byte[] data)  {
        MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
        md.update(data);
        byte byteData[] = md.digest();
        //convert the byte to hex format method 1
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < byteData.length; i++) {
        	sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
        //convert the byte to hex format method 2
        /*StringBuffer hexString = new StringBuffer();
    	for (int i=0;i<byteData.length;i++) {
    		String hex=Integer.toHexString(0xff & byteData[i]);
   	     	if(hex.length()==1) hexString.append('0');
   	     	hexString.append(hex);
    	}
    	System.out.println("Digest(in hex format):: " + hexString.toString());*/
    }
	
	public static boolean isServerPic(String serverId, byte[] photo, List<PicData> serverPicData) {
		for(PicData p : serverPicData)
		{
			if(p.serverId.equals(serverId))
			{
				if(p.picSize == photo.length) 
				// && ImageInfo.hash(photo).equals(p.picHash))
				{
					Log.i(tag, "Pic is same on server");
					return true;
				}
				else return false;
			}
		}
		return false;
	}
    
	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	@Override
	public String toString() {
		return "MIME Type : " + mimeType + "\t Width : " + width + "\t Height : " + height; 
	}
}
