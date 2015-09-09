package no.kantega.debug.agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import no.kantega.debug.util.DeadlockDetector;

import com.sun.jdi.ClassType;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.MonitorWaitEvent;

public class DeadlockDetectorAgent implements DeadlockDetectorMBean {

	private VirtualMachine vm;
	private ObjectReference terminateException;

	@Override
	public List<String> deadLockedThreads() {
		checkState();
		List<ThreadReference> deadlockedThreads = DeadlockDetector
				.deadlockedThreads(this.vm);
		ArrayList<String> threadNames = new ArrayList<String>(
				deadlockedThreads.size());
		for (final ThreadReference ref : deadlockedThreads) {
			threadNames.add(ref.name());
		}
		return threadNames;
	}

	@Override
	public String waitingForThread(String threadName) {
		checkState();

		return DeadlockDetector.waitingForThread(this.vm, threadName);
	}

	public Collection<String> deadLockedThreadsIfAny(
			final MonitorWaitEvent event) throws IncompatibleThreadStateException {
		checkState();
		Set<ThreadReference> recursionSet = new HashSet<ThreadReference>(
				Arrays.asList(event.thread()));
		if (DeadlockDetector.isDeadLocked(event.monitor().owningThread(),
				recursionSet)) {
			tryToCreateException(event.thread());
			Collection<String> sortedNames = new TreeSet<String>();
			for (ThreadReference reference : recursionSet) {
				sortedNames.add(reference.name());
			}
			return sortedNames;

		} else {
			return Collections.emptySet();
		}

	}

	private void tryToCreateException(ThreadReference thread) {
		if(this.terminateException == null) { 
			this.terminateException = createException(thread, "java.lang.IllegalThreadStateException");
		}
		
	}

	private void checkState() {
		if (!enabled()) {
			throw new IllegalStateException("Agent must be connected to VM");
		}
	}

	public void terminateThread(String threadName) {
		checkState();
		for (ThreadReference threadReference : this.vm.allThreads()) {
			if (threadReference.name().equals(threadName)) {

				try {
					threadReference.stop(this.terminateException);
				} catch (InvalidTypeException e) {
				}
			}
		}
	}

	private ObjectReference createException(
			final ThreadReference threadReference,
			final String exceptionClassName) {
		List<ReferenceType> exceptionClasses = this.vm
				.classesByName(exceptionClassName);
		if (!exceptionClasses.isEmpty()) {
			ClassType exceptionClass = (ClassType) exceptionClasses.get(0);
			for (Method method : exceptionClass.visibleMethods()) {
				if (method.isConstructor()
						&& method.argumentTypeNames().size() == 1
						&& method.argumentTypeNames().get(0)
								.equals("java.lang.String")) {
					try {
						return exceptionClass
								.newInstance(
										threadReference,
										method,
										Arrays.asList(this.vm
												.mirrorOf("Deadlocked thread terminated")),
										ClassType.INVOKE_SINGLE_THREADED);
					} catch (Exception e) {
					}
				}
			}

		}
		return createException(threadReference, "java.lang.RuntimeException");
	}

	@Override
	public boolean enabled() {

		return this.vm != null && this.vm.canGetCurrentContendedMonitor()
				&& this.vm.canGetMonitorInfo();
	}

}
