package no.kantega.debug.agent;

import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.ThreadReference;

public class WaitingThread {
	
	private final String name;
	private final ObjectReference monitor;
	private final String heldBy;
	
	public WaitingThread(final ThreadReference actualThread) throws IncompatibleThreadStateException {
		this.name = actualThread.name();
		this.monitor = new ObjectReference(actualThread.currentContendedMonitor());
		this.heldBy = actualThread.currentContendedMonitor().owningThread().name();
	}

	public String getName() {
		return name;
	}

	public ObjectReference getMonitor() {
		return monitor;
	}

	public String getHeldBy() {
		return heldBy;
	}

}
