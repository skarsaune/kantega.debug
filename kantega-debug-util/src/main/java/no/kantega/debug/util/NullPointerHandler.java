package no.kantega.debug.util;

import no.kantega.debug.execution.model.Expression;

import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.event.ExceptionEvent;

/**
 * Modify description of NullPointer to set more details about which method was
 * invoked
 * 
 * @author marska
 *
 */
public class NullPointerHandler {

	public static boolean enrichNullPointerException(ExceptionEvent event) {
		try {
			ObjectReference exception = event.exception();
			if (!"java.lang.NullPointerException".equals(exception.type()
					.name())) {
				return false; // only relevant for nullpointers
			}
			Field messageField = exception.referenceType().fieldByName(
					"detailMessage");
			//if message is already set, leave it
			if(exception.getValue(messageField) != null){
				return false;
			}
			Expression expression = DebugExpressionResolver
					.expressionAtLocation(event.location());
			exception.setValue(
					messageField,
					event.virtualMachine()
							.mirrorOf(
									"NullPointer occured invoking "
											+ expression));
			return true;
		} catch (Exception e) {
			System.err
					.println("Error occurred trying to describe NullPointerException : "
							+ e);
			e.printStackTrace();
			return false;

		}
	}

}
