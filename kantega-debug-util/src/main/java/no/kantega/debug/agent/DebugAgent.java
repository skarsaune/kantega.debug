package no.kantega.debug.agent;

import java.util.Iterator;

import no.kantega.debug.config.DebugAgentConfig;
import no.kantega.debug.util.NullPointerHandler;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.ExceptionRequest;

public class DebugAgent {

	private final DebugAgentConfig config;

	public DebugAgent(DebugAgentConfig config) {
		super();
		this.config = config;
	}

	public void handle(VirtualMachine vm) throws InterruptedException {
		if (config.shouldDiagnoseNullPointerExceptions()
				&& vm.canGetBytecodes() && vm.canGetConstantPool()) {

			ExceptionRequest createExceptionRequest = vm.eventRequestManager()
					.createExceptionRequest(
							vm.classesByName("java.lang.NullPointerException")
									.get(0), true, true);
			createExceptionRequest
					.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
			createExceptionRequest.enable();

		}

		EventSet set;
		while ((set = vm.eventQueue().remove()) != null) {
			Iterator<Event> events = set.eventIterator();
			while (events.hasNext()) {
				handleEvent(events.next());
			}
		}
	}

	private void handleEvent(Event event) {
		if (event instanceof ExceptionEvent) {
			try {
				if (NullPointerHandler
						.enrichNullPointerException((ExceptionEvent) event)
						&& this.config.shouldEmitWalkbackOnEvents()) {
					System.out.print(Walkback.printWalkback((LocatableEvent)event));
				}
				
			} finally {
				((ExceptionEvent) event).thread().resume();
			}
		}
	}

}
