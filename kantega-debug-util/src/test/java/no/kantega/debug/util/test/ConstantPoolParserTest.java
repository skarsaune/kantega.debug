package no.kantega.debug.util.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import junit.framework.Assert;
import no.kantega.debug.util.ConstantPoolParser;

import org.junit.Test;

public class ConstantPoolParserTest {

	@Test
	public void testParseConstantPool() throws IOException {
		final InputStream stream = getClass().getResourceAsStream("/" + this.getClass().getName().replace('.','/') + ".class");
		byte[] contents = new byte[stream.available()];
		stream.read(contents);
		//items will be limited to the last bit (max 255)
		int maxIndex = 122; //contents[9];
		List<Object> data = ConstantPoolParser.parseConstantPool(10, contents, maxIndex);//skip header of class file and go straight to constant pool
		Assert.assertEquals("Float constant", 19.0f ,  data.get(73));
		Assert.assertEquals("double constant", 17.0,  data.get(94));
		Assert.assertEquals("int constant", 25000000, data.get(102));
		Assert.assertEquals("long constant", 30l,  data.get(110));
		Assert.assertEquals("string constant", "string constant", data.get(117));
		Assert.assertEquals("method reference as string", "java.lang.StringBuilder.append(Ljava/lang/String;)Ljava/lang/StringBuilder;", data.get(42).toString());
		
		
		
	}

}
