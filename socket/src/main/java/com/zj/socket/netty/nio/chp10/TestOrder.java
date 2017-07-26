package com.zj.socket.netty.nio.chp10;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;

import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;

public class TestOrder {

	private IBindingFactory factory = null;
	private StringWriter writer = null;
	private StringReader reader = null;
	private static final String CHARSET_NAME = "UTF-8";
	
	
	private String encode2Xml(Order order) throws JiBXException, IOException {
		factory = BindingDirectory.getFactory(Order.class);
		writer = new StringWriter();
		IMarshallingContext mctx = factory.createMarshallingContext();
		mctx.setIndent(2);
		mctx.marshalDocument(order, CHARSET_NAME, null, writer);
		String xmlString = writer.toString();
		writer.close();
		System.out.println(xmlString.toString());
		return xmlString;
	}
	
	private Order decode2Order(String xmlBody) throws JiBXException {
		reader = new StringReader(xmlBody);
		IUnmarshallingContext uctx = factory.createUnmarshallingContext();
		Order order = (Order)uctx.unmarshalDocument(reader);
		return order;
	}
	
	public static void main(String[] args) throws JiBXException, IOException {
		TestOrder test = new TestOrder();
		
		Address billTo = new Address("阜安东路", "西华街", "SH", "state", "001000", "ShangHai");
		
		Customer customer = new Customer(1234L, "Zhang", "San", new ArrayList<String>(){{add("mid1");add("mid2"); add("mid3");}});
		
		Address shipTo = new Address("灌區門", "东大街", "Nanjing", "state", "012358", "JiangSu"); 
		
		Order order = new Order();
		order.setOrderNumber(123456L);
		order.setBillTo(billTo);
		order.setCustomer(customer);
		order.setShipping(Shipping.DOMESTIC_EXPRESS);
		order.setShipTo(shipTo);
		String body = test.encode2Xml(order);
		System.out.println("转换成xml文件后的内容: " + body);
		Order order2 = test.decode2Order(body);
		System.out.println("xml文件转换成java实体后的类 order = " + order2.toString());
	} 
}
