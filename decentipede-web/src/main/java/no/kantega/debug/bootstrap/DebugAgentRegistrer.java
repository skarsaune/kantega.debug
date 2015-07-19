package no.kantega.debug.bootstrap;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import no.kantega.debug.agent.DebugAgent;
import no.kantega.debug.inprocess.connect.AutomaticDebuggingConnector;

import org.slf4j.LoggerFactory;

public class DebugAgentRegistrer implements ServletContextListener {


	private DebugAgent agent;



	@Override
	public void contextInitialized(ServletContextEvent sce) {

		LoggerFactory.getLogger(this.getClass()).info("Registering debug agent in JMX");
		this.agent=new DebugAgent(new AutomaticDebuggingConnector());
	}


	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		LoggerFactory.getLogger(this.getClass()).info("Disconnecting debug agent");
		this.agent.stop();
		
	}
}
