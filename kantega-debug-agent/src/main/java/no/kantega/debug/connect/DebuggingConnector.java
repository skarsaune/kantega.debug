package no.kantega.debug.connect;

import java.io.IOException;

import no.kantega.debug.agent.DebugAgent;
import no.kantega.debug.agent.VirtualMachineProvider;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.spi.Connection;
import com.sun.tools.jdi.SocketTransportService;

public class DebuggingConnector {

	public static void main(String[] args) throws IOException,
			InterruptedException {

		new DebugAgent(new VirtualMachineProvider() {

			public VirtualMachine virtualMachine() {
				try {
					Connection connection = new SocketTransportService().attach("8765", 0, 0);
					VirtualMachine virtualMachine = Bootstrap
							.virtualMachineManager().createVirtualMachine(connection);
					
//					virtualMachine.setDebugTraceMode(VirtualMachine.TRACE_ALL);
					return virtualMachine;
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			}
		}).start();

	}

}
