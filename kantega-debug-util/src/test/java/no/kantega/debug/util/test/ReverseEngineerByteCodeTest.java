package no.kantega.debug.util.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import no.kantega.debug.decompile.ClassFileReverseEnginerer;

import org.junit.Test;

import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector.Argument;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.request.MethodEntryRequest;
import com.sun.jdi.request.MethodExitRequest;
import com.sun.jdi.request.VMDeathRequest;
import com.sun.tools.jdi.SunCommandLineLauncher;

public class ReverseEngineerByteCodeTest {

	@Test
	public void testSimpleClass() throws Exception {
		LaunchingConnector launcher = new SunCommandLineLauncher();
		Map<String, Argument> arguments = launcher.defaultArguments();
		arguments.get("main").setValue(
				"no.kantega.debug.util.test.DebugTestLauncher");
		final VirtualMachine vm = launcher.launch(arguments);
		ThreadReference mainThread = null;
		for (ThreadReference threadReference : vm.allThreads()) {
			if ("main".equals(threadReference.name())) {
				mainThread = threadReference;
				break;
			}
		}

		testReverseEngineerClass(vm, "java.io.Serializable", mainThread);
		testReverseEngineerClass(vm, "java.lang.AutoCloseable", mainThread);

		final MethodExitRequest stopInMain = vm.eventRequestManager()
				.createMethodExitRequest();
		stopInMain
				.addClassFilter("no.kantega.debug.util.test.DebugTestLauncher");
		stopInMain.enable();
		final MethodEntryRequest entry = vm.eventRequestManager()
				.createMethodEntryRequest();
		entry.addClassFilter("no.kantega.debug.util.test.DebugTestLauncher");
		entry.enable();
		final VMDeathRequest exitRequest = vm.eventRequestManager()
				.createVMDeathRequest();
		exitRequest.enable();
		vm.resume();
		EventSet remove = vm.eventQueue().remove();
		EventIterator iterator = remove.eventIterator();
		Event event = iterator.next();
		System.out.println(event);
		vm.resume();
	}

	private void testReverseEngineerClass(final VirtualMachine vm,
			final String className, final ThreadReference executeIn)
			throws IOException, InvalidTypeException, ClassNotLoadedException,
			IncompatibleThreadStateException, InvocationException {
		final ReferenceType type = vm.classesByName(className).get(0);
		ClassFileReverseEnginerer.reverseEngineerClassStructure(type,
		executeIn);
		final InputStream classFileStream = this.getClass()
				.getResourceAsStream(
						"/" + className.replace('.', '/') + ".class");
		byte[] classFileBytes = new byte[classFileStream.available()];
		classFileStream.read(classFileBytes);
		classFileStream.close();

		


	}
}
