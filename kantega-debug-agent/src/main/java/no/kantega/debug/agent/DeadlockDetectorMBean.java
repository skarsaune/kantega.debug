package no.kantega.debug.agent;

import java.util.List;

/**
 * Use JDI to check for deadlocked threads
 * @author marska
 *
 */
public interface DeadlockDetectorMBean {
	/**
	 * 
	 * @return List the names of deadlocked threads if any
	 */
	List<WaitingThread> deadLockedThreads();
	/**
	 * 
	 * @param threadName - the thread that is to be checked for 
	 * @return name of the thread that this thread is waiting for
	 */
	String waitingForThread(final String threadName);
	
	/**
	 *@return whether this functionality is possible (connected to VM and VM capable) 
	 */
	boolean enabled();
}
