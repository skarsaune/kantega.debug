package no.kantega.debug.util;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
/**
 * I use JDI's capability to query threads for monitors to detect whether several threads are in a deadlock due to 
 * monitor deadlocks 
 * @author marska
 *
 */
public class DeadlockDetector {
	
	/**
	 * Get a list of deadlocked threads if any
	 * @param vm
	 * @return
	 */
	public static List<ThreadReference> deadlockedThreads(final VirtualMachine vm) {
		final List<ThreadReference> lockedThreads=new LinkedList<ThreadReference>();
		for (ThreadReference threadReference : vm.allThreads()) {
			if(isDeadLocked(threadReference)) {
				lockedThreads.add(threadReference);
			}
		}
		return lockedThreads;
		
	}
	

	


	private static boolean isDeadLocked(final ThreadReference threadReference) {
		return isDeadLocked(threadReference, new HashSet<ThreadReference>());

	}
	
	public static boolean isDeadLocked(final ThreadReference threadReference, final Set<ThreadReference> recursionSet) {
		if(threadReference == null){//monitor chain broken
			return false;
		}
		
		if(recursionSet.contains(threadReference)) {
			return true;
		}
		

		
		recursionSet.add(threadReference);
		return isDeadLocked(waitingForThread(threadReference));
	}
	
	public static String waitingForThread(final VirtualMachine vm, final String waitingThread) {
		for (ThreadReference threadReference : vm.allThreads()) {
			if(threadReference.name().equals(waitingThread)) {
				final ThreadReference waitingForThread = waitingForThread(threadReference);
				return waitingForThread == null ? null : waitingForThread.name();
			}
		}
		throw new IllegalArgumentException("No thread named " + waitingThread);
	}

	private static ThreadReference waitingForThread(
			final ThreadReference threadReference) {
		ObjectReference waitingFor=null;
		try {
			waitingFor = threadReference.currentContendedMonitor();
		} catch (IncompatibleThreadStateException e) {
		}
		if(waitingFor == null) {
			return null;
		}
		else {
			try {
				return waitingFor.owningThread();
			} catch (IncompatibleThreadStateException e) {
				return null;
			}
		}
	}
	
	
}
