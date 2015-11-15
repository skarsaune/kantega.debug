package no.kantega.debug.agent;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public interface DebugAgentMBean {
	
	void start() throws InterruptedException, IOException;
	
	void stop();
	
	boolean monitorClass(String className);
	
	boolean stopMonitoringClass(String className);

	void setEmitWalkbacks(boolean emitWalkbacks);

	boolean isEmitWalkbacks();

	boolean isNullPointerDiagnosed();
	
	List<String> getMonitoredClasses();
	
	void setMonitoredClasses(List<String> classes);

	void setNullPointerDiagnosed(boolean nullPointerDiagnosed);
	
	boolean isRunning();

	void installAgent(int pid) throws Exception;
	
	String[] getWalkbacks();
	
	Collection<String> candidateClassesForFilter(String input);
}
