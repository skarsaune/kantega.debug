package no.kantega.debug.inprocess.connect;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import no.kantega.debug.agent.VirtualMachineProvider;

import org.slf4j.LoggerFactory;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector.Argument;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.jdi.ProcessAttachingConnector;
import com.sun.tools.jdi.SocketTransportService;

public class AutomaticDebuggingConnector implements VirtualMachineProvider {

	private VirtualMachine vm;

	public VirtualMachine virtualMachine() throws IOException,
			InterruptedException {

		vm = attemptToConnectByPid();
		if (vm == null) {
			vm = attemptToConnectByParsingCommandLine();
			if (vm == null) {
				return null;
			}
		}
		// vm.setDebugTraceMode(VirtualMachine.TRACE_ALL);
		// LoggerFactory.getLogger(this.getClass()).info("Successfully attached to VM of own process");
		return vm;
	}

	private VirtualMachine attemptToConnectByParsingCommandLine() {
		try {
			final List<String> arguments = ManagementFactory.getRuntimeMXBean()
					.getInputArguments();
			for (String argumentFromJmx : arguments) {
				StringTokenizer tokens = new StringTokenizer(argumentFromJmx);// in
																				// case
																				// JMX
																				// delivers
																				// several
																				// arguments
																				// in
																				// one
				while (tokens.hasMoreTokens()) {
					final String argument = tokens.nextToken();
					int jdwpIndex = argument.indexOf("agentlib:jdwp=");
					if (jdwpIndex > -1) {// check for occurrence
						// LoggerFactory.getLogger(this.getClass()).info("found jdwp entry on command line: {}",
						// argument);
						final String[] jdwpArguments = argument.substring(
								jdwpIndex + "agentlib:jdwp=".length()).split(
								",");// take the rest of the string
						final Properties jdwpProperties = new Properties();
						for (String jdwpArgument : jdwpArguments) {
							String[] keyValue = jdwpArgument.split("=");
							jdwpProperties
									.setProperty(keyValue[0], keyValue[1]);

						}
						if ("dt_socket".equals(jdwpProperties.get("transport"))
								&& "y".equals(jdwpProperties.get("server"))) {
							final String address = jdwpProperties
									.getProperty("address");
							// LoggerFactory.getLogger(this.getClass()).info("Attempt to connect to JVM at address {}",
							// address);
							return Bootstrap.virtualMachineManager()
									.createVirtualMachine(
											new SocketTransportService()
													.attach(address, 0, 0));
						}

					}

				}

			}
		} catch (Exception e) {// quick and dirty
			LoggerFactory.getLogger(this.getClass()).error(
					"Unable to connect to JVM by parsing command line", e);
		}
		return null;
	}

	/**
	 * Use PID to attach to process, (in fact JDI will then figure out protocol
	 * etc. for us)
	 * 
	 * @return
	 */
	private VirtualMachine attemptToConnectByPid() {

		final String processName = ManagementFactory.getRuntimeMXBean()
				.getName();
		final int attIndex = processName.indexOf('@');
		if (attIndex < 0) {// process name not of format pid@hostname
			return null;
		}
		String pidString = processName.substring(0, attIndex);
		
		setUpDebuggerIfRequrired(pidString);

		ProcessAttachingConnector connector = new ProcessAttachingConnector();
		final Map<String, Argument> arguments = connector.defaultArguments();
		if (arguments.get("pid").isValid(pidString)) {
			arguments.get("pid").setValue(pidString);
		} else {
			// LoggerFactory.getLogger(this.getClass()).error("Invalid process id format {}",
			// pidString);
			return null;
		}
		if (arguments.get("timeout").isValid("0")) {
			arguments.get("timeout").setValue("0");
		}
		try {
			LoggerFactory.getLogger(this.getClass()).info("Connecting to debugger with pid: {}" , pidString);
			VirtualMachine attach = connector.attach(arguments);
			return attach;
		} catch (Exception e) {
			// LoggerFactory.getLogger(this.getClass()).error("Unable to attach to process {}",
			// pidString, e);
			return null;
		}
	}

	private void setUpDebuggerIfRequrired(final String pid) {
		com.sun.tools.attach.VirtualMachine vm;
		try {
			LoggerFactory.getLogger(this.getClass()).info("Checking if debugger is listening in Java process with PID {}", pid);
			vm = com.sun.tools.attach.VirtualMachine.attach(pid);

			final Object jdwpProperties = vm.getAgentProperties().get("jdwp");
			if (jdwpProperties != null) {
				// logger.info("Debugger is already running {}",
				// jdwpProperties);
			} else {
				final String jdwpOptions = "transport=dt_socket,server=y,suspend=n,address="
						+ allocateFreePort();
				// logger.info("Setting up jdwp with options: {}", jdwpOptions);
				
				LoggerFactory.getLogger(this.getClass()).info("Starting jwdp in JVM {} with arguments {}", pid, jdwpOptions);
				vm.loadAgentLibrary("jdwp", jdwpOptions);
			}
		} catch (Exception e) {
			
		} 
	}

	/**
	 * Figure out a free port number
	 *
	 * @return
	 */
	private static int allocateFreePort() throws IOException {
		ServerSocket sock = null;
		try {
			return new ServerSocket(0).getLocalPort();
		} finally {
			if (sock != null) {
				try {
					sock.close();
				} catch (Exception e) {
					// ignore
				}
			}
		}
	}

}
