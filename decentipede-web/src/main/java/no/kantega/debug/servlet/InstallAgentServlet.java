package no.kantega.debug.servlet;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import no.kantega.debug.agent.DebugAgent;

import org.slf4j.LoggerFactory;

/**
 * I provide a HTTP callback in order to install the DeCentipede agent in a JVM with the designated PID
 */
public class InstallAgentServlet extends HttpServlet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -300483669907404175L;
	private DebugAgent agent;

	@Override
	public void init(ServletConfig config) throws ServletException {
		this.agent=(DebugAgent) config.getServletContext().getAttribute("Agent");
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse response)
			throws ServletException, IOException {
		
		final String pid = req.getParameter("pid");
		try {
			this.agent.installAgent(Integer.valueOf(pid));
		}  catch (Exception e) {
			LoggerFactory.getLogger(this.getClass()).error("Error starting agent in JVM with PID {}", pid);
			e.printStackTrace();
		}
		response.setContentType("text/plain");
		response.setStatus(200);
	}
	
	

}
