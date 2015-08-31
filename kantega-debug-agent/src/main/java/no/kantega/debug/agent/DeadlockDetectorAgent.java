package no.kantega.debug.agent;

import java.util.ArrayList;
import java.util.List;

import no.kantega.debug.util.DeadlockDetector;

import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;

public class DeadlockDetectorAgent implements DeadlockDetectorMBean {
	
	private VirtualMachine vm;
	
	

	@Override
	public List<String> deadLockedThreads() {
		if(!enabled()) {
			throw new IllegalStateException("Agent must be connected to VM");
		}
		List<ThreadReference> deadlockedThreads = DeadlockDetector.deadlockedThreads(this.vm);
		ArrayList<String> threadNames = new ArrayList<String>(deadlockedThreads.size());
		for(final ThreadReference ref : deadlockedThreads) {
			threadNames.add(ref.name());
		}
		return threadNames;
	}

	@Override
	public String waitingForThread(String threadName) {
		if(!enabled()) {
			throw new IllegalStateException("Agent must be connected to VM");
		}
		
		return DeadlockDetector.waitingForThread(this.vm, threadName);
	}

	@Override
	public boolean enabled() {
		
		return this.vm != null && this.vm.canGetCurrentContendedMonitor() && this.vm.canGetMonitorInfo();
	}

}
