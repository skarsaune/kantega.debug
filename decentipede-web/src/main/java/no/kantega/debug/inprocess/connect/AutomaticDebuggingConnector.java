package no.kantega.debug.inprocess.connect;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Map;

import no.kantega.debug.agent.VirtualMachineProvider;

import org.slf4j.LoggerFactory;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector.Argument;
import com.sun.tools.jdi.ProcessAttachingConnector;

public class AutomaticDebuggingConnector implements VirtualMachineProvider {
	
	private VirtualMachine vm;
	
	public VirtualMachine virtualMachine() throws IOException, InterruptedException {
		
		
		vm  = attemptToConnectByPid();
		if(vm == null) {
			return null;
		}
//		vm.setDebugTraceMode(VirtualMachine.TRACE_ALL);
		LoggerFactory.getLogger(this.getClass()).info("Successfully attached to VM of own process");
		return vm;
	}
	
	/**
	 * Use PID to attach to process, (in fact JDI will then figure out protocol etc. for us)
	 * @return
	 */
	private VirtualMachine attemptToConnectByPid() {
		
		final String processName = ManagementFactory.getRuntimeMXBean().getName();
		final int attIndex = processName.indexOf('@');
		if(attIndex < 0) {//process name not of format pid@hostname
			return null;
		}
		String pidString = processName.substring(0, attIndex);
		

		ProcessAttachingConnector connector = new ProcessAttachingConnector();
		@SuppressWarnings("unchecked")
		final Map<String, Argument> arguments=connector.defaultArguments();
		if(arguments.get("pid").isValid(pidString)) {
			arguments.get("pid").setValue(pidString);
		}
		else {
			LoggerFactory.getLogger(this.getClass()).error("Invalid process id format {}", pidString);
			return null;
		}
		if(arguments.get("timeout").isValid("0")) {
			arguments.get("timeout").setValue("0");
		}
		try {
			VirtualMachine attach = connector.attach(arguments);
			return attach;
		} catch (Exception e) {
			LoggerFactory.getLogger(this.getClass()).error("Unable to attach to process {}", pidString, e);
			return null;
		}
	}



}
