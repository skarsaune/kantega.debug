package no.kantega.debug.servlet;

import no.kantega.debug.agent.DebugAgent;
import no.kantega.debug.inprocess.connect.AutomaticDebuggingConnector;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * I serve two purposes:
 * 1. bootstrap decentiped in webapp context
 * 2. serving walkback files
 */
public class WalkbackServlet extends HttpServlet {
	
	private static final long serialVersionUID = 4708884638953627681L;
	private DebugAgent agent;

	@Override
	public void init(ServletConfig config) throws ServletException {
		LoggerFactory.getLogger(this.getClass()).info("Registering debug agent in JMX");
		this.agent=new DebugAgent(new AutomaticDebuggingConnector());
		config.getServletContext().setAttribute("Agent", this.agent);
		LoggerFactory.getLogger(this.getClass()).info(config.getServletContext().getContextPath());
		super.init(config);
	}
	
	
	@Override
	public void destroy() {
		LoggerFactory.getLogger(this.getClass()).info("Disconnecting debug agent");
		this.agent.stop();
		super.destroy();
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse response)
			throws ServletException, IOException {
		
		String fileName = req.getPathInfo();
		if(fileName.charAt(0)=='/') {
			fileName=fileName.substring(1);
		}
				
		File walkbackFile = agent.getWalkbackPrinter().getWalkbackFile(fileName);
		
		response.setContentType("text/plain");
		
		if(!walkbackFile.canRead() || !walkbackFile.isFile()) {
			response.getWriter().append("File not found on disk: ").append(fileName);
			response.sendError(500);
			return;
		}


//		response.addHeader("Content-Disposition", "attachment; filename=" + walkbackFile);
		int length = (int) walkbackFile.length();//these files are quite small
		response.setContentLength(length);

		FileInputStream fileInputStream = new FileInputStream(walkbackFile);
		byte[] content=new byte[length];
		fileInputStream.read(content);
		response.getOutputStream().write(content);
		fileInputStream.close();

	}
	
	

}
