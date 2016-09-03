package no.kantega.debug.decompile;

import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;

public class LocalVariableReflectionExtractor {
	private final LocalVariable delegate;

	public LocalVariableReflectionExtractor(LocalVariable delegate) {
		super();
		this.delegate = delegate;
	}
	
	public int getSlot() throws ReflectiveOperationException {
		if(JdiReflectionHacks.isEclipseJdi(this.delegate)) {
			return (Integer)JdiReflectionHacks.readField(this.delegate, "fSlot");
		} else {
			return (Integer)JdiReflectionHacks.readField(this.delegate, "slot");
		}
	}
	
	public int getStart() throws ReflectiveOperationException {
		if(JdiReflectionHacks.isEclipseJdi(this.delegate)) {
			return ((Long)JdiReflectionHacks.readField(this.delegate, "fCodeIndex")).intValue();
		} else {
			return (int)((Location)JdiReflectionHacks.readField(this.delegate, "scopeStart")).codeIndex();
		}
	}
	
	public int getLenght() throws ReflectiveOperationException {
		if(JdiReflectionHacks.isEclipseJdi(this.delegate)) {
			return (Integer)JdiReflectionHacks.readField(this.delegate, "fLength");
		} else {
			return (int)(((Long)((Location)JdiReflectionHacks.readField(this.delegate, "scopeEnd")).codeIndex()) - getStart());
		}
	}
	
}
