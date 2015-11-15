package no.kantega.debug.inprocess.connect;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import no.kantega.debug.agent.VirtualMachineProvider;

import org.slf4j.LoggerFactory;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector.Argument;
import com.sun.tools.jdi.ProcessAttachingConnector;
import com.sun.tools.jdi.SocketTransportService;

public class AutomaticDebuggingConnector implements VirtualMachineProvider {
	
	private VirtualMachine vm;
	
	public VirtualMachine virtualMachine() throws IOException, InterruptedException {
		
		
		vm  = attemptToConnectByPid();
		if(vm == null) {
			vm = attemptToConnectByParsingCommandLine();
			if(vm == null) {
				return null;
			}
		}
//		vm.setDebugTraceMode(VirtualMachine.TRACE_ALL);
		LoggerFactory.getLogger(this.getClass()).info("Successfully attached to VM of own process");
		return vm;
	}
	
	private VirtualMachine attemptToConnectByParsingCommandLine() {
		try {
		final List<String> arguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
		for (String argumentFromJmx : arguments) {
			StringTokenizer tokens=new StringTokenizer(argumentFromJmx);//in case JMX delivers several arguments in one
			while(tokens.hasMoreTokens()) {
				final String argument=tokens.nextToken();
				int jdwpIndex = argument.indexOf("agentlib:jdwp=");
				if(jdwpIndex > -1) {//check for occurrence
					LoggerFactory.getLogger(this.getClass()).info("found jdwp entry on command line: {}", argument);
					final String[] jdwpArguments=argument.substring(jdwpIndex + "agentlib:jdwp=".length()).split(",");//take the rest of the string
					final Properties jdwpProperties=new Properties();
					for (String jdwpArgument : jdwpArguments) {
						String[] keyValue = jdwpArgument.split("=");
						jdwpProperties.setProperty(keyValue[0], keyValue[1]);
						
					}
					if("dt_socket".equals(jdwpProperties.get("transport")) && "y".equals(jdwpProperties.get("server"))) {
						final String address = jdwpProperties.getProperty("address");
						LoggerFactory.getLogger(this.getClass()).info("Attempt to connect to JVM at address {}", address);
						return Bootstrap.virtualMachineManager().createVirtualMachine(new SocketTransportService().attach(address, 0, 0));
					}
		
					
				}
				
			}

		}
		} catch(Exception e) {//quick and dirty
			LoggerFactory.getLogger(this.getClass()).error("Unable to connect to JVM by parsing command line", e);
		}
		return null;
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
