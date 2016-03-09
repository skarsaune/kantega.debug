package no.kantega.debug.agent;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Interface to control the DeCentipede debugging agent
 * @author marska
 *
 */
public interface DebugAgentMBean {
	
	/**
	 * Start and attach the debugging connection to my JVM
	 * @throws InterruptedException
	 * @throws IOException
	 */
	void start() throws InterruptedException, IOException;
	
	/**
	 * Stop the agent, detach the debugging connection from the JVM and clean up
	 */
	void stop();
	
	/**
	 * 
	 * @param className - add a class to the list of monitored classes 
	 * @return whether any change took place
	 */
	boolean monitorClass(String className);
	
	/**
	 * 
	 * @param className - class that should no longer be monitored for instances
	 * @return true if any change took place (the specified class was in the list of classes)
	 */
	boolean stopMonitoringClass(String className);

	/**
	 * 
	 * @param emitWalkbacks - specify whether walkback files should be emit on errors
	 */
	void setEmitWalkbacks(boolean emitWalkbacks);

	/**
	 * 
	 * @return whether walkback files should be produced on errors
	 */
	boolean isEmitWalkbacks();

	/**
	 * 
	 * @return whether NullPointerExceptions are intercepted and analyzed for cause
	 */
	boolean isNullPointerDiagnosed();
	
	/**
	 * 
	 * @return - fully qualified names of Java classes that are monitored for instance counts
	 */
	List<String> getMonitoredClasses();
	
	/**
	 * @param classes - fully qualified names of Java classes that are monitored for instance counts
	 */
	void setMonitoredClasses(List<String> classes);

	/**
	 * 
	 * @param nullPointerDiagnosed - specify whether NullPointerExceptions should be intercepted and information added
	 */
	void setNullPointerDiagnosed(boolean nullPointerDiagnosed);
	
	/**
	 * 
	 * @return boolean - whether the agent is running and connected to the VM
	 */
	boolean isRunning();

	/**
	 * Install the decentipede agent in a target JVM on this machine with the given pid
	 * @param pid
	 * @throws Exception
	 */
	void installAgent(int pid) throws Exception;
	
	/**
	 * 
	 * @return List of all walkbacks on disk
	 */
	String[] getWalkbacks();
	
	/**
	 * Get the contents of a walkback file
	 * @param walkback
	 * @return
	 * @throws IOException 
	 */
	String getWalkback(String walkback) throws IOException;
	
	/**
	 * Get potential matches among loaded classes that are valid according to the filter
	 * @param input
	 * @return
	 */
	Collection<String> candidateClassesForFilter(String input);

	/**
	 * Get suggestions for resource hierarchy tops that may be relevant for checking subclasses
	 * For instance java.sql.Connection
	 * @return list of fully qualified class names 
	 */
	List<String> getResourceRootCandidates();

	/**
	 * 
	 * @param resourceRoot root of resource hierarchy for instance java.sql.Connection
	 * @return Implementor hierarchy including instance count for each
	 */
	Map<String, Long> getImplementorsAndCounts(String resourceRoot);
	
	/**
	 * 
	 * @return List with information about deadlocked threads if any
	 */
	List<WaitingThread> getDeadlocks();
	
	/**
	 *@param threadName String name of thread 
	 *@return Dump text walkback of thread with name threadName 
	 */
	String dumpThreadWalkback(String threadName);
	
	
	
}
