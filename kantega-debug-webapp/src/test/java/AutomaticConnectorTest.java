import static org.junit.Assert.*;

import java.io.IOException;

import junit.framework.Assert;
import no.kantega.debug.inprocess.connect.AutomaticDebuggingConnector;

import org.junit.Test;


public class AutomaticConnectorTest {

	@Test
	public void test() throws IOException, InterruptedException {
		assertNotNull("VM should not be Null" , new AutomaticDebuggingConnector().virtualMachine());
	}

}
