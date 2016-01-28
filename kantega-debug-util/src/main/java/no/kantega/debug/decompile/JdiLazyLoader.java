package no.kantega.debug.decompile;

import java.util.NoSuchElementException;

import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.struct.lazy.LazyLoader;

import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;

/**
 * Emulate a loader by reading information out of JDI instead of class files
 * @author marska
 *
 */
public class JdiLazyLoader extends LazyLoader {
	
	private ReferenceType source;

	public JdiLazyLoader(final ReferenceType source) {
		super(null);
		this.source = source;
	}
	
	@Override
	public byte[] loadBytecode(StructMethod mt, int codeFullLength) {
		for (final Method method : this.source.methods()) {
			if(method.name().equals(mt.getName()) && method.signature().equals(mt.getDescriptor())) {
				return method.bytecodes();
			}
		}
		throw new NoSuchElementException(this.source + " does not define any method " + mt.getName() + mt.getDescriptor());
	}

}
