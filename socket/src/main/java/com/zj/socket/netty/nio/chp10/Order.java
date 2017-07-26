package com.zj.socket.netty.nio.chp10;
/**
 * 订单
 * @author 123
 *
 */
public class Order {

	private long orderNumber;
	private Customer customer;
	private Address billTo;
	private Shipping shipping;
	private Address shipTo;
	private float total;

	public Order() {
		super();
	}

	public long getOrderNumber() {
		return orderNumber;
	}

	public void setOrderNumber(long orderNumber) {
		this.orderNumber = orderNumber;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public Address getBillTo() {
		return billTo;
	}

	public void setBillTo(Address billTo) {
		this.billTo = billTo;
	}

	public Shipping getShipping() {
		return shipping;
	}

	public void setShipping(Shipping shipping) {
		this.shipping = shipping;
	}

	public Address getShipTo() {
		return shipTo;
	}

	public void setShipTo(Address shipTo) {
		this.shipTo = shipTo;
	}

	public float getTotal() {
		return total;
	}

	public void setTotal(float total) {
		this.total = total;
	}

}
