package com.zj.socket.netty.nio.chp10;

import java.util.List;

public class Customer {

	private long customerNumber;
	private String firstName;
	private String lastName;
	private List<String> middleNames;

	public Customer() {
		super();
	}

	public Customer(long customerNumber, String firstName, String lastName,
			List<String> middleNames) {
		super();
		this.customerNumber = customerNumber;
		this.firstName = firstName;
		this.lastName = lastName;
		this.middleNames = middleNames;
	}

	public long getCustomerNumber() {
		return customerNumber;
	}

	public void setCustomerNumber(long customerNumber) {
		this.customerNumber = customerNumber;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public List<String> getMiddleNames() {
		return middleNames;
	}

	public void setMiddleNames(List<String> middleNames) {
		this.middleNames = middleNames;
	}

}
