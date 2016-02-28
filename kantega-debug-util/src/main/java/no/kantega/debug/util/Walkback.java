package no.kantega.debug.util;

import java.util.Collections;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.event.LocatableEvent;

public class Walkback {

	public static String printWalkback(LocatableEvent event) {
		StringBuilder builder=new StringBuilder();
		try {
			for(StackFrame frame : event.thread().frames()) {
				printFrame(frame, builder);
			}
			ensureThreadIsResumed(event);			
			return builder.toString();


		} catch (Exception e) {
			return "unable to print stack trace: " + e;
		}
		
	}

	/**
	 * In case we have piled up too many suspend requests, ensure thread is resumed
	 * @param event
	 */
	private static void ensureThreadIsResumed(LocatableEvent event) {
		while(event.thread().suspendCount() > 0) {
			event.thread().resume();
		}
	}

	private static void printFrame(StackFrame frame, StringBuilder builder) {
		final Method method = frame.location().method();

		ObjectReference thisObject = frame.thisObject();
		builder.append("\n at ").append(method.declaringType().name()).append('.').append(method.name()).append('(');
		if(sourceName(frame) != null) {
			builder.append(sourceName(frame));
		}
		if(frame.location().lineNumber()>-1) {
			builder.append(':').append(frame.location().lineNumber());
		}
		builder.append(")\n");
		if(method.isNative()) {
			builder.append("<native method>\n");
			return;
		}
		
		if(thisObject != null) {
			appendObject("\tthis", thisObject, frame.thread(), builder);
		}
		try {
			for (LocalVariable localVariable : frame.visibleVariables()) {
				builder.append(localVariable.isArgument() ? "\targ: " : "\ttmp: ");
				appendvalue(localVariable.name(), frame.getValue(localVariable), frame.thread(), builder);
			}
		} catch (AbsentInformationException e) {
			builder.append("Unable to list local variables\n");
		}
		
		

		
	}

	private static String sourceName(StackFrame frame) {
		try {
			return frame.location().sourceName();
		} catch (AbsentInformationException e) {
			return null;
		}
	}

	private static void appendvalue(String name, Value value,
			ThreadReference thread, StringBuilder builder) {
		if(value instanceof ObjectReference) {
			appendObject(name, (ObjectReference)value, thread, builder);
		} else if(value == null) {
			builder.append(name).append(" = null\n");
		}
		else {
			builder.append(value.type().name()).append(' ').append(name).append(" = ").append(value.toString()).append('\n');
		}
		
	}

	private static void appendObject(final String name, ObjectReference object, ThreadReference thread,
			StringBuilder builder) {
		builder.append(name).append(" = ").append(object.type().name()).append("(Id=").append(object.uniqueID()).append(")\n");
//		builder.append(" = ");
//		printObject(object, thread);
	}

	private static Object printObject(ObjectReference object, ThreadReference thread) {
		Method method = object.virtualMachine().classesByName("java.lang.Object").get(0).methodsByName("toString").get(0);
		try {
			if(thread.suspendCount()<2) {//increase suspendcount to ensure that thread does not resume after invoking method
				thread.suspend();
			}
			Value asString = object.invokeMethod(thread, method, Collections.<Value> emptyList(), ObjectReference.INVOKE_SINGLE_THREADED);
			return asString.toString();
		} catch (Exception e) {
			return "Unable to print";
		} 
	}
	
}
