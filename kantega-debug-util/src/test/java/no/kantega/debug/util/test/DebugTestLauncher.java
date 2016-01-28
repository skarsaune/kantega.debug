package no.kantega.debug.util.test;

import java.io.IOException;

public class DebugTestLauncher {
	
	static SimpleClass simpleClass=new SimpleClass();

	public static void main(String[] args) throws IOException {
		System.out.println("in");
		//load classes to be able to compare bytecodes
		System.in.read();
		System.out.println("out");
	}

}
