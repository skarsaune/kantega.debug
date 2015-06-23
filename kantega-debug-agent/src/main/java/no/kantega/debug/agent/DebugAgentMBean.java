package no.kantega.debug.agent;

import java.io.IOException;

public interface DebugAgentMBean {
	
	void start() throws InterruptedException, IOException;
	
	void stop();

	public abstract void setEmitWalkbacks(boolean emitWalkbacks);

	public abstract boolean isEmitWalkbacks();

	public abstract boolean isNullPointerDiagnosed();

	public abstract void setNullPointerDiagnosed(boolean nullPointerDiagnosed);
	
	

}
