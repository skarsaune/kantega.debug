package no.kantega.debug.agent;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import no.kantega.debug.util.NullPointerHandler;

import org.slf4j.LoggerFactory;

import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StringReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.ExceptionRequest;

public class DebugAgent implements DebugAgentMBean {

	private static final String DEBUG_AGENT_JMX_NAME = "no.kantega.debug:type=DebugAgent";
	private static final String INSTANCE_COUNTER_JMX_NAME = "no.kantega.debug:type=Instances";
	private ExceptionRequest nullPointerExceptionRequest;
	private boolean running = false;
	private VirtualMachine vm;
	private boolean nullPointerDiagnosed=true;
	private boolean emitWalkbacks=true;
	final private VirtualMachineProvider provider;
	private final WalkbackPrinter walkbackPrinter=new WalkbackPrinter();
	private InstanceCounter counter=new InstanceCounter();

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
			mbs.registerMBean(this, new ObjectName(DEBUG_AGENT_JMX_NAME));
			mbs.registerMBean(this.counter, new ObjectName(INSTANCE_COUNTER_JMX_NAME));
		} catch (Exception e) {
			LoggerFactory.getLogger(this.getClass()).error(
					"Unable to register myself in JMX", e);
		}

	}

	public void start() throws InterruptedException, IOException {
		this.vm = this.provider.virtualMachine();
		if (this.vm == null) {
			this.running = false;
			LoggerFactory.getLogger(this.getClass()).error(
					"Could not start manager as there is no available VM");
			return;
		}
		this.counter.setVirtualMachine(this.vm);
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
	}

	public void stop() {
		this.running = false;
		if (vm != null) {
			this.nullPointerExceptionRequest = null;
			this.vm.dispose();
			this.vm = null;
			this.counter.setVirtualMachine(null);
		}
	}

	@Override
	public boolean isNullPointerDiagnosed() {
		return this.nullPointerDiagnosed;
	}

	private void runEventLoop() {
		this.running = true;
		while (running) {
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
			final File walkbackFile = this.walkbackPrinter.printWalkback((LocatableEvent) event);
			if(walkbackFile.exists() && event instanceof ExceptionEvent) {
				reportAboutWalkback((ExceptionEvent) event, walkbackFile);
			}
			
		}

	}

	private void reportAboutWalkback(ExceptionEvent event, File walkbackFile) {
		ObjectReference exception = event.exception();

		
		try {
			Field messageField = exception.referenceType().fieldByName(
					"detailMessage");
			
			String messageString=currentMessage(exception, messageField);
			messageString += " details can be found in walkback " + walkbackFile.getAbsolutePath();

			exception.setValue(
					messageField,
					event.virtualMachine()
							.mirrorOf(messageString));
		} catch (Exception e) {
			LoggerFactory.getLogger(this.getClass()).warn("Unable to attach walkback information to exception ", e);
		} 
		
	}

	private String currentMessage(ObjectReference exception, Field messageField) {
		final Value message = exception.getValue(messageField);
		if(message instanceof StringReference) {
			return ((StringReference)message).value();
		}
		else {
			return "";
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
		return this.running;
	}

	@Override
	public void monitorClass(String className) {
		this.counter.addClass(className);
		
	}

	@Override
	public List<String> getMonitoredClasses() {
		
		return new ArrayList<String>(this.counter.monitoredClasses());
	}

	@Override
	public void setMonitoredClasses(List<String> classes) {
		this.counter.setMonitoredClasses(classes);
		
	}

	public WalkbackPrinter getWalkbackPrinter() {
		return this.walkbackPrinter;
	}	
	
	public String[] getWalkbacks() {
		return this.getWalkbackPrinter().getWalkbacks();
	}	
	
	public Collection<String> candidateClassesForFilter(final String input) {
		return this.counter.candidateClassesForFilter(input);
	}

}
