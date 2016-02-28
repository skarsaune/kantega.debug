package no.kantega.debug.decompile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.struct.attr.StructGeneralAttribute;
import org.jetbrains.java.decompiler.struct.consts.ConstantPool;
import org.jetbrains.java.decompiler.struct.consts.PooledConstant;
import org.jetbrains.java.decompiler.struct.consts.PrimitiveConstant;
import org.jetbrains.java.decompiler.util.DataInputFullStream;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;

public class ClassFileReverseEnginerer {

	public static byte[] reverseEngineerClassStructure(final ReferenceType declaringType, final ThreadReference executeIn)
			throws IOException, InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException, InvocationException {

		final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		final DataOutputStream out = new DataOutputStream(buffer);

		out.writeShort(0xcafe);
		out.writeShort(0xbabe);
		out.writeShort(declaringType.minorVersion());
		out.writeShort(declaringType.majorVersion());
		out.writeShort(declaringType.constantPoolCount());
		byte[] constantPoolBytes = declaringType.constantPool();
//		no.kantega.debug.bytecode.ConstantPool pool = new no.kantega.debug.bytecode.ConstantPool(constantPoolBytes);
		out.write(constantPoolBytes);
		out.writeShort(declaringType.modifiers());
		out.writeShort(thisClass(declaringType));
		out.writeShort(superClass(declaringType));
		writeInterfaces(out, declaringType);
		writeFields(out, declaringType);
		//no methods
		out.writeShort(0);
		//no attributes
		out.writeShort(0);
//		writeMethods(out, declaringType, pool, executeIn);
//		writeAttributes(out, declaringType);
		return buffer.toByteArray();
	}
	
	public static StructClass reverseEngineeringBasis(final ReferenceType type) throws Exception {
		return new StructClass(reverseEngineerClassStructure(type, null), true, new JdiLazyLoader(type));
	} 
	
	
	public static StructMethod reverseEngineer(final Method method) throws Exception {
		final StructClass classSkeleton = reverseEngineeringBasis(method.declaringType());
		return new StructMethod(new DataInputFullStream(reverseEngineerMethodStructure(method, classSkeleton.getPool())), classSkeleton);
		
	}

//	private static void writeAttributes(DataOutputStream out,
//			ReferenceType declaringType) throws IOException {
////		no.kantega.debug.bytecode.ConstantPool constantPool = new no.kantega.debug.bytecode.ConstantPool(
////				declaringType.constantPool());
//		int sourceFile=-1;
//		try {
//			final String sourceName = declaringType.sourceName();
//			sourceFile = constantPool.indexOfConstantPrintedAs(sourceName);
//		} catch (Exception e) {
//			out.writeShort(0);
//			return;
//		}
//		int sourceFileAttrName = constantPool.indexOfConstantPrintedAs("SourceFile");
//		if (sourceFile > 0 && (sourceFileAttrName > 0)) {
//			out.writeShort(1);
//			out.writeShort(sourceFileAttrName);
//			out.writeInt(2);
//			out.writeShort(sourceFile);
//		} else {
//			out.writeShort(0);
//		}
//	}

//	private static void writeMethods(DataOutputStream out,
//			ReferenceType declaringType, ConstantPool pool, final ThreadReference executeIn) throws IOException, InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException, InvocationException {
//
//		List<Method> methods = declaringType.methods();
//		out.writeShort(methods.size());
//		for (final Method method : methods) {
//			MethodToJavaLangReflectMethod.javaLangReflectMethodFor(method, executeIn);
//			out.writeShort(method.modifiers());
//			out.writeShort(pool.indexOfConstantPrintedAs(method.name()));
//			out.writeShort(pool.indexOfConstantPrintedAs(method.signature()));
//			
//			out.writeShort(0);
//		}
//		
//
//	}
	
	private static byte[] reverseEngineerMethodStructure(final Method method,
			final ConstantPool pool) throws IOException, AbsentInformationException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		DataOutputStream out=new DataOutputStream(buffer);
		out.writeShort(method.modifiers());
		out.writeShort(indexOfConstantPrintedAs(pool, method.name()));
		out.writeShort(indexOfConstantPrintedAs(pool, method.signature()));
		//add code attribute to make decompiler attempt to read bytecodes
		out.writeShort(1);
		out.writeShort(indexOfConstantPrintedAs(pool, StructGeneralAttribute.ATTRIBUTE_CODE));
		//skip various information
		for(int i=0;i<3;i++) {
			out.writeShort(0);
		}
		int variableCount = method.variables().size();
		if(!method.isStatic()) {
			variableCount++;
		}
		out.writeShort(variableCount);
		byte[] bytecodes = method.bytecodes();
		out.writeInt(bytecodes.length);
		out.write(bytecodes);
		//skip exception table
		out.writeShort(0);
		//skip code attributes
		out.writeShort(0);
		return buffer.toByteArray();
	}

	private static int indexOfConstantPrintedAs(final ConstantPool pool,
			final String signature) {
		for(int i=1;i<pool.size();i++) {
			PooledConstant constant = pool.getConstant(i);
			if(constant instanceof PrimitiveConstant && constant.type == CodeConstants.CONSTANT_Utf8) {
				if(((PrimitiveConstant) constant).getString().equals(signature)) {
					return i;
				}
			}
		}
		return -1;
	}

	public static Value resourcePath(final ReferenceType type, final ThreadReference thread) throws InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException, InvocationException {
		final String signature = type.signature();
		final String internalResourcePath = "/" + signature.substring(1, signature.length() - 1) + ".class";
		StringReference mirrorInternalPath = type.virtualMachine().mirrorOf(internalResourcePath);
		return type.classObject().invokeMethod(thread, type.virtualMachine().classesByName("java.lang.Class").get(0).methodsByName("getResource").get(0), Collections.singletonList(mirrorInternalPath), ObjectReference.INVOKE_SINGLE_THREADED);
	} 

	private static void writeFields(DataOutputStream out,
			ReferenceType declaringType) throws IOException {
		out.writeShort(0);

	}

	private static void writeInterfaces(DataOutputStream out,
			ReferenceType declaringType) throws IOException {
		out.writeShort(0);
	}

	private static int superClass(ReferenceType declaringType) {

		return 2;
	}

	private static int thisClass(ReferenceType declaringType) {

		return 1;
	}

//	private static int accessFlags(ReferenceType declaringType) {
//
//		int flags = 0;
//		if (declaringType.isPublic()) {
//			flags |= 0x0001;
//		}
//		if (declaringType.isFinal()) {
//			flags |= 0x0010;
//		}
//		if (declaringType instanceof InterfaceType) {
//			flags |= 0x0200;
//		}
//		if (declaringType.isAbstract()) {
//			flags |= 0x0400;
//		}
//		return flags;
//	}

	public static org.jetbrains.java.decompiler.struct.consts.ConstantPool parseConstantPool(final ReferenceType type)
			throws IOException {

		final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		final DataOutputStream out = new DataOutputStream(buffer);
		out.writeShort(type.constantPoolCount());
		out.write(type.constantPool());
		ByteArrayInputStream bis = new ByteArrayInputStream(
				buffer.toByteArray());
		DataInputStream dis = new DataInputStream(bis);

		org.jetbrains.java.decompiler.struct.consts.ConstantPool constantPool = new org.jetbrains.java.decompiler.struct.consts.ConstantPool(
				dis);
		return constantPool;

	}

}
