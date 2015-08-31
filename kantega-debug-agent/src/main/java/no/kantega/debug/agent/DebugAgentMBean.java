package no.kantega.debug.agent;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public interface DebugAgentMBean {
	
	void start() throws InterruptedException, IOException;
	
	void stop();
	
	void monitorClass(String className);

	void setEmitWalkbacks(boolean emitWalkbacks);

	boolean isEmitWalkbacks();

	boolean isNullPointerDiagnosed();
	
	List<String> getMonitoredClasses();
	
	void setMonitoredClasses(List<String> classes);

	void setNullPointerDiagnosed(boolean nullPointerDiagnosed);
	
	boolean isRunning();
	
	String[] getWalkbacks();
	
	Collection<String> candidateClassesForFilter(String input);
}
