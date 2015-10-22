package model;

import java.util.Date;
import java.util.logging.Logger;

import com.darcy.hcsar.server.MessageUtils;

public class IncidentModel {
	private static final Logger log = Logger.getLogger(IncidentModel.class.getName());  
	private String nature;
	private Date timeOut;
	private String address;
	private String cross;
	private String business;
	private String notes;
	private String addtAddress;

	public String getNature() {
		return this.nature;
	}

	public void setNature(String nature) {
		this.nature = nature;
	}

	public Date getTimeOut() {
		return this.timeOut;
	}

	public void setTimeOut(Date timeOut) {
		this.timeOut = timeOut;
	}

	public String getAddress() {
		return this.address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getCross() {
		return this.cross;
	}

	public void setCross(String cross) {
		this.cross = cross;
	}

	public String getBusiness() {
		return this.business;
	}

	public void setBusiness(String business) {
		this.business = business;
	}

	public String getNotes() {
		return this.notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public String getAddtAddress() {
		return this.addtAddress;
	}

	public void setAddtAddress(String addtAddress) {
		log.severe("Setting addt to "+addtAddress);
		this.addtAddress = addtAddress;
	}

	public String toString() {
		String output = new String();
		if (this.nature != null) {
			output = output + "\nNature: " + this.nature;
		}
		if (this.address != null) {
			output = output + "\nAddress: " + this.address;
		}
		if (this.cross != null) {
			output = output + "\nCross: " + this.cross;
		}
		if (this.business != null) {
			output = output + "\nBusiness: " + this.business;
		}
		if (this.timeOut != null) {
			output = output + "\nTimeout: " + this.timeOut;
		}
		if (this.addtAddress != null) {
			output = output + "\nInfo: " + this.addtAddress;
		}

		return output;
	}
}