package com.nfbank.socket.communication.communication;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DemoTest {

	public static void main(String[] args) {
		List<String> list = new ArrayList<String>(){{
			add("a");
			add("b");
			add("c");
			add("d");
			add("e");
		}};
		Iterator<String> it = list.iterator();
		while(it.hasNext()){
			String next = it.next();
			System.out.println(next);
			it.remove();
			System.out.println(list.size());
			System.out.println("---------------");
		}
	}
}
