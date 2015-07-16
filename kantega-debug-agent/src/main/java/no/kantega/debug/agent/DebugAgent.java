package no.kantega.debug.agent;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Iterator;
import java.util.concurrent.Executors;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import no.kantega.debug.util.NullPointerHandler;
import no.kantega.debug.util.Walkback;

import org.slf4j.LoggerFactory;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.ExceptionRequest;

public class DebugAgent implements DebugAgentMBean {

	private ExceptionRequest nullPointerExceptionRequest;
	private boolean shouldRun = false;
	private VirtualMachine vm;
	private boolean nullPointerDiagnosed=true;
	private boolean emitWalkbacks=true;
	final private VirtualMachineProvider provider;

	@Override
	public void setNullPointerDiagnosed(boolean nullPointerDiagnosed) {
		this.nullPointerDiagnosed = nullPointerDiagnosed;
		nullPointerRequest().setEnabled(nullPointerDiagnosed);
	}

	public DebugAgent(VirtualMachineProvider provider) {
		super();
		this.provider = provider;
		registerMyself();
	}

	private void registerMyself() {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		try {
			ObjectName name = new ObjectName("no.kantega.debug:type=DebugAgent");
			mbs.registerMBean(this, name);
		} catch (Exception e) {
			LoggerFactory.getLogger(this.getClass()).error(
					"Unable to register myself in JMX", e);
		}

	}

	public void start() throws InterruptedException, IOException {
		this.vm = this.provider.virtualMachine();
		if (this.vm == null) {
			this.shouldRun = false;
			LoggerFactory.getLogger(this.getClass()).error(
					"Could not start manager as there is no available VM");
			return;
		}
		if (this.isNullPointerDiagnosed() && vm.canGetBytecodes()
				&& vm.canGetConstantPool()) {

			nullPointerRequest().enable();

		}
		Executors.newFixedThreadPool(1).execute(new Runnable() {

			@Override
			public void run() {
				runEventLoop();

			}
		});
		runEventLoop();
	}

	public void stop() {
		this.shouldRun = false;
		if (vm != null) {
			this.vm.dispose();
			this.vm = null;
		}
	}

	@Override
	public boolean isNullPointerDiagnosed() {
		return this.nullPointerDiagnosed;
	}

	private void runEventLoop() {
		this.shouldRun = true;
		while (shouldRun) {
			try {

				EventSet set = vm.eventQueue().remove();
				if (set == null) {
					break;
				}
				Iterator<Event> events = set.eventIterator();
				while (events.hasNext()) {
					handleEvent(events.next());
				}
			} catch (Exception e) {
				LoggerFactory.getLogger(this.getClass()).error(
						"Error occurred processing debug events", e);

			}
		}
	}

	private ExceptionRequest nullPointerRequest() {
		if (this.nullPointerExceptionRequest != null) {
			return this.nullPointerExceptionRequest;
		}

		this.nullPointerExceptionRequest = vm.eventRequestManager()
				.createExceptionRequest(
						vm.classesByName("java.lang.NullPointerException").get(
								0), true, true);
		nullPointerExceptionRequest
				.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);

		return nullPointerExceptionRequest;
	}

	private void handleEvent(Event event) {
		if (event instanceof ExceptionEvent) {
			try {
				if (NullPointerHandler
						.enrichNullPointerException((ExceptionEvent) event)) {
					reportEvent(event);
				}

			} finally {
				((ExceptionEvent) event).thread().resume();
			}
		}
	}

	private void reportEvent(Event event) {
		if (event instanceof LocatableEvent && this.isEmitWalkbacks()) {
			System.out.println(Walkback.printWalkback((LocatableEvent) event));
		}

	}

	@Override
	public boolean isEmitWalkbacks() {
		return this.emitWalkbacks;
	}

	@Override
	public void setEmitWalkbacks(boolean emitWalkbacks) {
		this.emitWalkbacks = emitWalkbacks;
	}

	@Override
	public boolean isRunning() {
		return this.shouldRun;
	}

	@Override
	public String toggleString() {
		return this.shouldRun ? "stop" : "start";
	}

	@Override
	public void toggle() throws InterruptedException, IOException {
		if(this.shouldRun) {
			stop();
		} else {
			start();
		}
		
	}
	
	
	
	

}
