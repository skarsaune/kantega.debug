package no.kantega.debug.agent;

import java.io.IOException;

public interface DebugAgentMBean {
	
	void start() throws InterruptedException, IOException;
	
	void stop();

	void setEmitWalkbacks(boolean emitWalkbacks);

	boolean isEmitWalkbacks();

	boolean isNullPointerDiagnosed();

	void setNullPointerDiagnosed(boolean nullPointerDiagnosed);
	
	boolean isRunning();
	
	String toggleString();
	
	void toggle() throws InterruptedException, IOException;

}
