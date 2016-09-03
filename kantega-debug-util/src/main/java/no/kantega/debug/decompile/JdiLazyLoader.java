package no.kantega.debug.decompile;

import java.io.IOException;
import java.util.NoSuchElementException;

import no.kantega.debug.log.Logging;

import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.struct.consts.ConstantPool;
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
	public ConstantPool loadPool(String classname) {
		if(classname.equals(this.source.name()) || classname.equals(this.source.name().replace('.','/'))) {
			try {
				return ClassFileReverseEnginerer.parseConstantPool(this.source);
			} catch (IOException e) {
				Logging.warn(this.getClass(), "Unable to access constant pool of class: " + classname, e);
			}
		}
		return super.loadPool(classname);
	}
	
	@Override
	public byte[] loadBytecode(StructMethod mt, int codeFullLength) {
		for (final Method method : this.source.methods()) {
			if(method.name().equals(mt.getName()) && method.signature().equals(mt.getDescriptor())) {
				byte[] bytecodes = method.bytecodes();
				byte[] includingEmptyExceptionTable = new byte[bytecodes.length + 2];
				System.arraycopy(bytecodes, 0, includingEmptyExceptionTable, 0, bytecodes.length);
				return includingEmptyExceptionTable;
			}
		}
		throw new NoSuchElementException(this.source + " does not define any method " + mt.getName() + mt.getDescriptor());
	}

}
