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

	private final VirtualMachine vm;
	private ObjectReference terminateException;

	public DeadlockDetectorAgent(final VirtualMachine vm) {
		this.vm = vm;
	}

	public List<WaitingThread> deadLockedThreads() {
		checkState();
		List<ThreadReference> deadlockedThreads = DeadlockDetector
				.deadlockedThreads(this.vm);
		ArrayList<WaitingThread> threadNames = new ArrayList<WaitingThread>(
				deadlockedThreads.size());
		for (final ThreadReference ref : deadlockedThreads) {
			try {
				threadNames.add(new WaitingThread(ref));
			} catch (IncompatibleThreadStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return threadNames;
	}

	public String waitingForThread(String threadName) {
		checkState();

		return DeadlockDetector.waitingForThread(this.vm, threadName);
	}

	public Collection<String> deadLockedThreadsIfAny(
			final MonitorWaitEvent event)
			throws IncompatibleThreadStateException {
		checkState();
		Collection<String> sortedNames = new TreeSet<String>();
		for (ThreadReference reference : DeadlockDetector
				.deadlockedThreads(event.virtualMachine())) {
			sortedNames.add(reference.name());
		}
		return sortedNames;

	}

	private void tryToCreateException(ThreadReference thread) {
		if (this.terminateException == null) {
			this.terminateException = createException(thread,
					"java.lang.IllegalThreadStateException");
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

	public boolean enabled() {

		return this.vm != null && this.vm.canGetCurrentContendedMonitor()
				&& this.vm.canGetMonitorInfo();
	}

}
