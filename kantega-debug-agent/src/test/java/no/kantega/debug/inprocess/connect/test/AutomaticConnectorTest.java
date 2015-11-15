package no.kantega.debug.inprocess.connect.test;

import no.kantega.debug.inprocess.connect.AutomaticDebuggingConnector;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;


public class AutomaticConnectorTest {

	@Test
	public void test() throws IOException, InterruptedException {
		Assert.assertNotNull("VM should not be Null", new AutomaticDebuggingConnector().virtualMachine());
	}

}
