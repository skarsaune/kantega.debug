package no.kantega.debug.decompile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.ClassesProcessor;
import org.jetbrains.java.decompiler.main.ClassesProcessor.ClassNode;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.TextBuffer;
import org.jetbrains.java.decompiler.main.collectors.BytecodeMappingTracer;
import org.jetbrains.java.decompiler.main.collectors.CounterContainer;
import org.jetbrains.java.decompiler.main.collectors.ImportCollector;
import org.jetbrains.java.decompiler.main.rels.MethodProcessorRunnable;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarProcessor;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructContext;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.struct.attr.StructGeneralAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructLineNumberTableAttribute;
import org.jetbrains.java.decompiler.struct.consts.ConstantPool;
import org.jetbrains.java.decompiler.struct.consts.PooledConstant;
import org.jetbrains.java.decompiler.struct.consts.PrimitiveConstant;
import org.jetbrains.java.decompiler.struct.gen.MethodDescriptor;
import org.jetbrains.java.decompiler.util.DataInputFullStream;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;

public class ClassFileReverseEnginerer {

	public static byte[] reverseEngineerClassStructure(
			final ReferenceType declaringType, final ThreadReference executeIn)
			throws IOException, InvalidTypeException, ClassNotLoadedException,
			IncompatibleThreadStateException, InvocationException {

		final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		final DataOutputStream out = new DataOutputStream(buffer);

		out.writeShort(0xcafe);
		out.writeShort(0xbabe);
		out.writeShort(declaringType.minorVersion());
		out.writeShort(declaringType.majorVersion());
		writeConstantPool(declaringType, out);
		out.writeShort(declaringType.modifiers());
		ConstantPool constantPool = parseConstantPool(declaringType);
		out.writeShort(thisClass(declaringType, constantPool));
		out.writeShort(superClass(declaringType, constantPool));
		writeInterfaces(out, declaringType);
		writeFields(out, declaringType);
		// no methods
		out.writeShort(0);
		// no attributes
		out.writeShort(0);
		// writeMethods(out, declaringType, pool, executeIn);
		// writeAttributes(out, declaringType);
		return buffer.toByteArray();
	}

	private static void writeConstantPool(final ReferenceType declaringType,
			final DataOutputStream out) throws IOException {
		out.writeShort(declaringType.constantPoolCount());
		byte[] constantPoolBytes = declaringType.constantPool();
		out.write(constantPoolBytes);
	}

	public static StructClass reverseEngineeringBasis(final ReferenceType type)
			throws Exception {
		return new StructClass(reverseEngineerClassStructure(type, null), true,
				new JdiLazyLoader(type));
	}

	public static StructMethod reverseEngineer(final Method method)
			throws Exception {
		final StructClass classSkeleton = reverseEngineeringBasis(method
				.declaringType());
		return new StructMethod(
				new DataInputFullStream(reverseEngineerMethodStructure(method,
						classSkeleton.getPool())), classSkeleton);

	}

	public static String decompileMethod(Location location) throws Exception {


		TextBuffer decompiledContent = methodToTextBuffer(location);
		final String methodLineByLine = decompiledContent.toString();
		//gobble up any initial blank lines created by line number offsets
		int begin=0;
		while(methodLineByLine.charAt(begin) == '\n' || methodLineByLine.charAt(begin) == '\r') {
			begin++;
		}
		//start from first non linebreak character
		return replaceVarNames(methodLineByLine.substring(begin),
				location.method());

	}

	private static TextBuffer methodToTextBuffer(Location location)
			throws Exception, IOException {
		Method method = location.method();
		StructMethod reverseEngineered = ClassFileReverseEnginerer
				.reverseEngineer(method);
		reverseEngineered.expandData();
		DecompilerContext.setCounterContainer(new CounterContainer());
		DecompilerContext.setProperty(
				DecompilerContext.CURRENT_METHOD_DESCRIPTOR,
				MethodDescriptor.parseDescriptor(method.signature()));
		DecompilerContext.setProperty(DecompilerContext.CURRENT_METHOD,
				reverseEngineered);
		DecompilerContext.setProperty(DecompilerContext.CURRENT_CLASS,
				reverseEngineered.getClassStruct());
		DecompilerContext
				.setVarNamesCollector(new JdiVarNamesCollector(method));

		// DecompilerContext.setBytecodeSourceMapper(bytecodeSourceMapper);
		StructContext context = new StructContext(null, null,
				new JdiLazyLoader(method.declaringType()));
		DecompilerContext.setStructContext(context);
		DecompilerContext.setClassProcessor(new ClassesProcessor(context));
		DecompilerContext.setImportCollector(new ImportCollector(new ClassNode(
				ClassNode.CLASS_ROOT, reverseEngineered.getClassStruct())));
		BytecodeMappingTracer tracer = new BytecodeMappingTracer();
		for (StructGeneralAttribute attribute : reverseEngineered
				.getAttributes()) {
			if (attribute instanceof StructLineNumberTableAttribute) {
				tracer.setLineNumberTable((StructLineNumberTableAttribute) attribute);
			}
		}

		TextBuffer toJava = MethodProcessorRunnable.codeToJava(
				reverseEngineered, new VarProcessor()).toJava(1, tracer);

		for (final Location line : method.allLineLocations()) {
			toJava.setLineMapping(line.lineNumber(), (int) line.codeIndex());
		}
		return toJava;
	}

	// private static BytecodeSourceMapper byteCodeMapperFor(Location location)
	// throws AbsentInformationException {
	// final BytecodeSourceMapper bytecodeSourceMapper = new
	// BytecodeSourceMapper();
	// for (Location loc : location.method().allLineLocations()) {
	// bytecodeSourceMapper.addMapping(location.declaringType().name(),
	// location.method().name(), (int) loc.codeIndex(),
	// loc.lineNumber());
	// }
	// return bytecodeSourceMapper;
	// }

	// private static void writeAttributes(DataOutputStream out,
	// ReferenceType declaringType) throws IOException {
	// // no.kantega.debug.bytecode.ConstantPool constantPool = new
	// no.kantega.debug.bytecode.ConstantPool(
	// // declaringType.constantPool());
	// int sourceFile=-1;
	// try {
	// final String sourceName = declaringType.sourceName();
	// sourceFile = constantPool.indexOfConstantPrintedAs(sourceName);
	// } catch (Exception e) {
	// out.writeShort(0);
	// return;
	// }
	// int sourceFileAttrName =
	// constantPool.indexOfConstantPrintedAs("SourceFile");
	// if (sourceFile > 0 && (sourceFileAttrName > 0)) {
	// out.writeShort(1);
	// out.writeShort(sourceFileAttrName);
	// out.writeInt(2);
	// out.writeShort(sourceFile);
	// } else {
	// out.writeShort(0);
	// }
	// }

	// private static void writeMethods(DataOutputStream out,
	// ReferenceType declaringType, ConstantPool pool, final ThreadReference
	// executeIn) throws IOException, InvalidTypeException,
	// ClassNotLoadedException, IncompatibleThreadStateException,
	// InvocationException {
	//
	// List<Method> methods = declaringType.methods();
	// out.writeShort(methods.size());
	// for (final Method method : methods) {
	// MethodToJavaLangReflectMethod.javaLangReflectMethodFor(method,
	// executeIn);
	// out.writeShort(method.modifiers());
	// out.writeShort(pool.indexOfConstantPrintedAs(method.name()));
	// out.writeShort(pool.indexOfConstantPrintedAs(method.signature()));
	//
	// out.writeShort(0);
	// }
	//
	//
	// }

	public static String decompileCurrentLine(Location location)
			throws Exception {

		String decompiled = decompileMethod(location);
		if(location.method().allLineLocations().size() > 0) {
			//offset from first line of method to my line
			int offset=location.lineNumber() - location.method().allLineLocations().get(0).lineNumber();
			final String[] byLines=decompiled.split("\n");
			if(offset >= 0 && offset<byLines.length) {
				return byLines[offset];
			}
		}
		//if not return entire method
		return decompiled;
	}

	/**
	 * Hack: As I have problems feeding in variable information, use rudimentary
	 * string replacements to fix names afterwards
	 */
	private static String replaceVarNames(String decompiledContent,
			final Method method) throws AbsentInformationException,
			ClassNotLoadedException {

		final List<LocalVariable> variables = method.variables();
		for (int reverse = variables.size(); reverse > 0; reverse--) {
			final LocalVariable variable = variables.get(reverse - 1);
			int varIndex = reverse;
			if (method.isStatic()) {
				varIndex--;
			}
			// substitute variable declarations and usages
			decompiledContent = decompiledContent.replace("<unknown> var"
					+ varIndex,
					simplify(variable.typeName()) + " " + variable.name());
			decompiledContent = decompiledContent.replace("var" + varIndex,
					variable.name());
		}

		if (!method.isStatic()) {
			decompiledContent = decompiledContent.replace("var0.", "");// remove
																		// "this"
																		// qualifying
																		// references
			// remaining var0 will be passing of this reference
			decompiledContent = decompiledContent.replace("var0", "this");// remove
																			// "this"
																			// qualifying
																			// references
		}
		decompiledContent = decompiledContent.replace("(<unknown>)", "");// remove
																			// redundant
																			// casts
		decompiledContent = decompiledContent.replace("this$0.", "");// implicit
																		// reference
																		// to
																		// enclosing
																		// instance

		return decompiledContent;
	}

	/**
	 * Strip name of qualifier to make more readable
	 * 
	 * @return Simple name
	 */
	private static String simplify(final String typeName) {
		int lastDot = typeName.lastIndexOf('.');
		if (lastDot > -1) {
			return typeName.substring(lastDot + 1);
		}
		return typeName;
	}

	private static byte[] reverseEngineerMethodStructure(final Method method,
			final ConstantPool pool) throws IOException,
			AbsentInformationException, ReflectiveOperationException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(buffer);
		out.writeShort(method.modifiers());
		out.writeShort(indexOfConstantPrintedAs(pool, method.name()));
		out.writeShort(indexOfConstantPrintedAs(pool, method.signature()));
		// add code attribute to make decompiler attempt to read bytecodes
		out.writeShort(1);
		out.writeShort(indexOfConstantPrintedAs(pool,
				StructGeneralAttribute.ATTRIBUTE_CODE));
		// attribute lenght and max_stack not used by decompiler
		for (int i = 0; i < 3; i++) {
			out.writeShort(0);
		}
		int variableCount = method.variables().size();
		if (!method.isStatic()) {
			variableCount++;
		}
		out.writeShort(variableCount);
		byte[] bytecodes = method.bytecodes();
		out.writeInt(bytecodes.length);
		out.write(bytecodes);
		// skip exception table
		out.writeShort(0);
		// https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.12
		final int indexOfLineNumberTable = indexOfConstantPrintedAs(pool,
				StructGeneralAttribute.ATTRIBUTE_LINE_NUMBER_TABLE);
		short numberOfAttributes = 0;
		List<Location> locations = method.allLineLocations();
		boolean hasLinenumbers = false; // not picked up by decompiler anyway :
										// indexOfLineNumberTable > -1
		// && !locations.isEmpty();
		if (hasLinenumbers) {
			numberOfAttributes++;
		}
		final int indexOfVariables = indexOfConstantPrintedAs(pool,
				StructGeneralAttribute.ATTRIBUTE_LOCAL_VARIABLE_TABLE);
		final List<LocalVariable> variables = method.variables();
		final boolean needsVariableInformation = false; // not picked up anyway
														// variables.size() > 0
		// && indexOfVariables > -1
		// && JdiReflectionHacks.canRetrievePrivateInfo(method);
		if (needsVariableInformation) {
			numberOfAttributes++;
		}
		out.writeShort(numberOfAttributes);
		if (hasLinenumbers) {
			out.writeShort(indexOfLineNumberTable);
			out.writeInt(2 + locations.size() * 4);
			out.writeShort(locations.size());
			for (final Location location : locations) {
				out.writeShort((short) location.codeIndex());
				out.writeShort(location.lineNumber());
			}
		}
//		if (hasLinenumbers && needsVariableInformation) {
//			out.writeShort(indexOfVariables);
//			out.writeShort(2 + variableCount * 10);
//			out.writeShort(variableCount);
//
//			int index = 0;
//			if (!method.isStatic()) {
//				addVariable(out, pool, 0, bytecodes.length - 1, "this",
//						index++, method.declaringType().signature());
//			}
//			for (final LocalVariable variable : method.variables()) {
//				LocalVariableReflectionExtractor extractor = new LocalVariableReflectionExtractor(
//						variable);
//				addVariable(out, pool, extractor.getStart(),
//						extractor.getLenght(), variable.name(),
//						extractor.getSlot(), variable.signature());
//			}
//		}
		return buffer.toByteArray();
	}

//	private static void addVariable(DataOutputStream out, ConstantPool pool,
//			int start, int lenght, String name, int slot, String signature)
//			throws IOException {
//		out.writeShort(start);
//		out.writeShort(lenght);
//		out.writeShort(indexOfConstantPrintedAs(pool, name));
//		out.writeShort(indexOfConstantPrintedAs(pool, signature));
//		out.writeShort(slot);
//	}

	private static int indexOfConstantPrintedAs(final ConstantPool pool,
			final String signature) {
		for (int i = 1; i < pool.size(); i++) {
			PooledConstant constant = pool.getConstant(i);
			if (constant instanceof PrimitiveConstant
					&& constant.type == CodeConstants.CONSTANT_Utf8) {
				if (((PrimitiveConstant) constant).getString()
						.equals(signature)) {
					return i;
				}
			}
		}
		return -1;
	}

	public static Value resourcePath(final ReferenceType type,
			final ThreadReference thread) throws InvalidTypeException,
			ClassNotLoadedException, IncompatibleThreadStateException,
			InvocationException {
		final String signature = type.signature();
		final String internalResourcePath = "/"
				+ signature.substring(1, signature.length() - 1) + ".class";
		StringReference mirrorInternalPath = type.virtualMachine().mirrorOf(
				internalResourcePath);
		return type.classObject().invokeMethod(
				thread,
				type.virtualMachine().classesByName("java.lang.Class").get(0)
						.methodsByName("getResource").get(0),
				Collections.singletonList(mirrorInternalPath),
				ObjectReference.INVOKE_SINGLE_THREADED);
	}

	private static void writeFields(DataOutputStream out,
			ReferenceType declaringType) throws IOException {
		out.writeShort(0);

	}

	private static void writeInterfaces(DataOutputStream out,
			ReferenceType declaringType) throws IOException {
		out.writeShort(0);
	}

	private static int superClass(ReferenceType declaringType,
			ConstantPool constantPool) {
		if (declaringType instanceof ClassType) {
			return indexOfConstantPrintedAs(
					constantPool,
					qualifiedClassName(((ClassType) declaringType).superclass()));
		}
		return 2;
	}

	private static int thisClass(ReferenceType declaringType,
			ConstantPool constantPool) {
		return indexOfConstantPrintedAs(constantPool,
				qualifiedClassName(declaringType));
	}

	private static String qualifiedClassName(ReferenceType type) {
		String qualifiedClassName = type.signature();
		// due to different JDI implementations, accept both JNI style and
		// without
		if (qualifiedClassName.startsWith("L")
				&& qualifiedClassName.endsWith(";")) {
			qualifiedClassName = qualifiedClassName.substring(1,
					qualifiedClassName.length() - 1);
		}
		return qualifiedClassName;
	}

	// private static int accessFlags(ReferenceType declaringType) {
	//
	// int flags = 0;
	// if (declaringType.isPublic()) {
	// flags |= 0x0001;
	// }
	// if (declaringType.isFinal()) {
	// flags |= 0x0010;
	// }
	// if (declaringType instanceof InterfaceType) {
	// flags |= 0x0200;
	// }
	// if (declaringType.isAbstract()) {
	// flags |= 0x0400;
	// }
	// return flags;
	// }

	public static org.jetbrains.java.decompiler.struct.consts.ConstantPool parseConstantPool(
			final ReferenceType type) throws IOException {

		final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		final DataOutputStream out = new DataOutputStream(buffer);
		writeConstantPool(type, out);
		ByteArrayInputStream bis = new ByteArrayInputStream(
				buffer.toByteArray());
		DataInputStream dis = new DataInputStream(bis);

		org.jetbrains.java.decompiler.struct.consts.ConstantPool constantPool = new org.jetbrains.java.decompiler.struct.consts.ConstantPool(
				dis);
		return constantPool;

	}

}
