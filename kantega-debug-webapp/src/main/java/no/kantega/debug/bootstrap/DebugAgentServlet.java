package no.kantega.debug.bootstrap;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import no.kantega.debug.agent.DebugAgent;
import no.kantega.debug.inprocess.connect.AutomaticDebuggingConnector;

import org.slf4j.LoggerFactory;

public class DebugAgentServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1456597169795493261L;
	private DebugAgent agent;
	
	@Override
	public void init() throws ServletException {
		LoggerFactory.getLogger(this.getClass()).info("Starting {}" , this.getClass().getSimpleName());
		this.agent=new DebugAgent(new AutomaticDebuggingConnector());
//		try {
//			this.agent.start();
//		} catch (Exception e) {
//			LoggerFactory.getLogger(this.getClass()).error("Unable to start debug agent", e);
//		}
	}

	
	@Override
	public void destroy() {
		this.agent.stop();
	}
}
