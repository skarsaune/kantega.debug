package no.kantega.debug.agent.jvm;

import org.slf4j.LoggerFactory;

import no.kantega.debug.agent.DebugAgent;
import no.kantega.debug.inprocess.connect.AutomaticDebuggingConnector;

/**
 * Java Agent that will install the decentipede debug agent in a target VM
 * either by command line switch or dynamic attach
 * 
 * @author marska
 *
 */
public class DecentipedeJvmAgent {


    private static DebugAgent agent;

    /**
	 * Entry point when invoked from Java command line
	 * @param agentArgs
	 */
    public static void premain(String agentArgs) {
        startAgent();
    }

    /**
     * Entry point for the agent, using dynamic attach
     * (this is post VM initialisation attachment, via com.sun.attach)
     *
     * @param agentArgs arguments as given on the command line
     */
    public static void agentmain(String agentArgs) {
    	startAgent();
    }

	private static DebugAgent startAgent() {
		if("true".equalsIgnoreCase(System.getProperty("decentipede.installed", "false"))) {
			LoggerFactory.getLogger(DecentipedeJvmAgent.class).info("Decentipede JVM agent is already installed");
		}
		LoggerFactory.getLogger(DecentipedeJvmAgent.class).info("Installing Decentipede JVM agent");
		agent = new DebugAgent(new AutomaticDebuggingConnector());
		System.setProperty("decentipede.installed", "true");
		LoggerFactory.getLogger(DecentipedeJvmAgent.class).info("Decentipede JVM agent installed and registered in JMX");
		return agent;
	}
}
