package no.kantega.debug.agent;

import java.io.IOException;

import com.sun.jdi.VirtualMachine;

/**
 * I am able to connect to a Virtual Machine over JDWP
 */
public interface VirtualMachineProvider {
	/**
	 * 
	 * @return a VirtualMachine for the DebugAgent to use
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public VirtualMachine virtualMachine() throws IOException, InterruptedException;
}
