package com.softmeter.utils;

import com.example.skhalid.softmetersimulation.Constants;

import java.net.DatagramPacket;

public class IOMessage {

	private String[] packetArray;
	private String header;
	private String body;
	private int mMsgType;
	private String mMsgTag; // mmddyyyyhhmmssSSS
	private String deviceID;
	private String vehicleNum;



	public IOMessage(String Packetdata) {

		String temp = Packetdata;
		this.packetArray = temp.split(Character.toString(Constants.BODYSEPARATOR));
		this.header = packetArray[0];
		String[] headerArray = header.split("\\" + Character.toString(Constants.COLSEPARATOR));
		this.body = packetArray[1].split("\\" + Constants.EOT)[0];

		this.mMsgType = Integer.valueOf(headerArray[0]);
		this.mMsgTag = headerArray[1];
        this.deviceID = headerArray[2];
        this.vehicleNum = headerArray[3];

    }// Constructor

	public void setType(int mMsgType) {
		this.mMsgType = mMsgType;
	}

	public String getHeader() {
		return header;
	}

	public int getType() {
		return Integer.valueOf(mMsgType);
	}

	public void setTag(String mMsgTag) {
		this.mMsgTag = mMsgTag;
	}

	public String getTag() {
		return mMsgTag;
	}

	public void setDeviceID(String mDeviceID) {
		this.deviceID = mDeviceID;
	}

	public String getDeviceID() {
		return deviceID;
	}

	public void setVehicleNum(String mVehicleNum) {
		this.vehicleNum = mVehicleNum;
	}

	public String getVehicleNum() {
		return vehicleNum;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getBody() {
		return body;
	}


}
