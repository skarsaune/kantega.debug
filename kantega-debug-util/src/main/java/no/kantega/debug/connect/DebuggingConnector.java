package no.kantega.debug.connect;

import java.io.IOException;

import no.kantega.debug.agent.DebugAgent;
import no.kantega.debug.config.DebugAgentConfig;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.spi.Connection;
import com.sun.tools.jdi.SocketTransportService;

public class DebuggingConnector {
	
	public static void main(String[] args) throws IOException, InterruptedException {
		Connection connection = new SocketTransportService().attach("8765", 0, 0);
		VirtualMachine virtualMachine = Bootstrap.virtualMachineManager().createVirtualMachine(connection);
		new DebugAgent(new DebugAgentConfig() {
			
			public boolean shouldEmitWalkbackOnEvents() {
				return true;
			}
			
			public boolean shouldDiagnoseNullPointerExceptions() {
				return true;
			}
		}).handle(virtualMachine);

		virtualMachine.dispose();
	}


}
