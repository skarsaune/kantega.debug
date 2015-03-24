package no.kantega.debug.util;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.sun.jdi.ReferenceType;


public class DebugHelper {
	
	public static byte[] classPrefix(final ReferenceType type) {
		
		List<Byte> byteCollection = new ArrayList<Byte>();
		byteCollection.add(hexToByte("ca"));
		byteCollection.add(hexToByte("fe"));
		byteCollection.add(hexToByte("ba"));
		byteCollection.add(hexToByte("be"));
		addIntegerAs2byte(type.minorVersion(), byteCollection);
		addIntegerAs2byte(type.majorVersion(), byteCollection);
		addIntegerAs2byte(type.constantPoolCount(), byteCollection);
		
		byte[] result = new byte[byteCollection.size()];
		for(int i=0;i<result.length;i++) {
			result[i] = byteCollection.get(i);
		}
			
		return result;
	}
	
	public static byte[] simuateClassForLocation(final ReferenceType type) {
		
		final byte[] constants = type.constantPool();
		
		final byte[] prefix=classPrefix(type);
		
		final byte[] joined = Arrays.copyOf(prefix, constants.length + prefix.length);
		
		System.arraycopy(constants, 0, joined, prefix.length, joined.length);
		
		return joined;
		
	}
	
	private static byte hexToByte(final String hexString) {
		return (byte)Integer.parseInt(hexString, 16);
	}
	
	private static void addIntegerAs2byte(final int integer, final Collection<Byte> appendTo) {
		appendTo.add((byte)0);
		appendTo.add((byte)integer);
	}

}
