package no.kantega.debug.bytecode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.kantega.debug.execution.model.Expression;

/**
 * Standalone utility without external dependencies to parse a constantPool byte
 * array returned from com.sun.jdi.ReferenceType.constantPool based on JVM
 * specification of class file format, for instance:
 * http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html
 * 
 * @author marska
 *
 */
public class ConstantPool implements ConstantStore {
	private int maxIndex = Integer.MAX_VALUE; //no limit to parsing unless specified otherwise
	private int startFromIndex = 0; //read from beginning of byte array unless specified otherwise
	private byte[] raw;

	public static class InvokeDynamicImpl extends ConstantPoolStructure{

		private int nameAndTypeIndex;
		private int bootstrapIndex;

		public InvokeDynamicImpl(ConstantStore resolver, int bootstrapIndex , int nameAndTypeIndex) {
			super(resolver);
			this.bootstrapIndex = bootstrapIndex;
			this.nameAndTypeIndex = nameAndTypeIndex;
		}
		
		public int getBootstrapIndex() {
			return bootstrapIndex;
		}
		
		public NameAndTypeImpl getNameAndType() {
			return (NameAndTypeImpl) getReference(nameAndTypeIndex);
		}

	}

	public static class MethodTypeImpl extends StringReferenceImpl {

		public MethodTypeImpl(ConstantStore resolver, int index) {
			super(resolver, index);
		}
		

	}

	public static class FieldReferenceImpl extends MemberReferenceImpl {

		public FieldReferenceImpl(ConstantStore resolver,
				final int classIndex, final int nameAndTypeIndex) {
			super(resolver, classIndex, nameAndTypeIndex);
		}

	}

	public static class MethodReferenceImpl extends MemberReferenceImpl implements Expression {

		public MethodReferenceImpl(ConstantStore resolver,
				final int classIndex, final int nameAndTypeIndex) {
			super(resolver, classIndex, nameAndTypeIndex);
		}
		
		@Override
		public String toString() {

			return methodReferenceString();
		}


	}

	public static class MemberReferenceImpl extends ConstantPoolStructure implements Expression {

		private int classIndex;
		private int nameAndTypeIndex;

		public MemberReferenceImpl(ConstantResolver resolver, int classIndex,
				int nameAndTypeIndex) {
			super(resolver);
			this.classIndex = classIndex;
			this.nameAndTypeIndex = nameAndTypeIndex;
		}

		public ClassReferenceImpl getClassReference() {
			return (ClassReferenceImpl) getReference(this.classIndex);
		}
		 

		public NameAndTypeImpl getNameAndType() {
			return (NameAndTypeImpl) getReference(nameAndTypeIndex);
		}
		
		public String toString() {
			return getClassReference() + "." + getNameAndType();
		}
		
		/**
		 * For methods, convert JNI type names to Java source references
		 * @return
		 */
		protected String methodReferenceString() {
			return getClassReference() + "." + getNameAndType().getName() + JniTypeToSourceTranslator.javaSignatureFromJni(getNameAndType().getDescriptor());
		}


	}

	public static class InterfaceMethodReferenceImpl extends
			MemberReferenceImpl {

		public InterfaceMethodReferenceImpl(ConstantResolver resolver,
				final int classIndex, final int nameAndTypeIndex) {
			super(resolver, classIndex, nameAndTypeIndex);
		}
		
		@Override
		public String toString() {

			return methodReferenceString();
		}

	}

	public static class ClassReferenceImpl extends StringReferenceImpl {

		public ClassReferenceImpl(ConstantResolver resolver, int index) {
			super(resolver, index);
		}
		
		public String className() {
			return getValue().replace('/', '.');
		}
		
		public String toString() {
			return className();
		}

	}

	public static class MethodHandle extends ConstantPoolStructure {

		private int referenceIndex;
		private byte type;

		public MethodHandle(ConstantResolver resolver, byte type, int index) {
			super(resolver);
			this.type = type;
			this.referenceIndex = index;
		}

		public byte getType() {
			return this.type;
		}

		public ConstantPoolStructure getReference() {
			return (ConstantPoolStructure) getReference(this.referenceIndex);
		}

	}

	public static class ConstantPoolStructure {

		private ConstantResolver resolver;

		public ConstantPoolStructure(ConstantResolver resolver) {
			this.resolver = resolver;
		}

		protected Object getReference(final int index) {
			return this.resolver.getConstant(index);
		}
		
		protected String internalNameToSource(final String internalName) {
			switch(internalName.charAt(0)) {
			case 'V':
				return "void";
			case 'L':
				return internalName.substring(1).replace('/', '.');
			default:
				return internalName;
			}
		}

	}

	public static class NameAndTypeImpl extends ConstantPoolStructure {

		private int nameIndex;
		private int descriptorIndex;

		public NameAndTypeImpl(ConstantResolver resolver, int nameIndex,
				int descriptorIndex) {
			super(resolver);
			this.nameIndex = nameIndex;
			this.descriptorIndex = descriptorIndex;
		}

		public String getName() {
			return (String) this.getReference(nameIndex);
		}

		public String getDescriptor() {
			return (String) this.getReference(descriptorIndex);
		}
		
		public String toString() {
			return getName() + getDescriptor();
		}

	}

	static class StringReferenceImpl extends ConstantPoolStructure {

		private int index;

		public StringReferenceImpl(ConstantResolver resolver, int index) {
			super(resolver);
			this.index = index;
		}

		public String getValue() {
			return (String) this.getReference(this.index);
		}
		
		public String toString() {
			return getValue();
		}

	}

	private static Collection<ValueParser> allParsers = Arrays.asList(
			new Utf8Parser(), new IntegerParser(), new FloatParser(),
			new LongParser(), new DoubleParser(), new NameAndTypeParser(),
			new MethodTypeParser(), new InterfaceMethodReferenceParser(),
			new MethodReferenceParser(), new FieldReferenceParser(),
			new StringReferenceParser(), new ClassConstParser(), new InvokeDynamicParser(), new MethodHandleParser(), new MethodTypeParser());
	private static Map<Byte, ValueParser> parserMap = buildParserMap(allParsers);

	public static class InvokeDynamicParser extends ValueParser {

		@Override
		int parseNext(ConstantStore data, byte[] raw, int startFrom) {
			data.addConstant(new InvokeDynamicImpl(data, readIndex(raw,
					startFrom), readIndex(raw, startFrom + 2)));
			return startFrom + 4;
		}

		@Override
		byte getTag() {
			return 18;
		}

	}

	public static class MethodTypeParser extends ValueParser {

		@Override
		int parseNext(ConstantStore destination, byte[] raw, int startFrom) {
			destination.addConstant(new MethodTypeImpl(destination, readIndex(raw, startFrom)));
			return startFrom + 2;
		}

		@Override
		byte getTag() {
			return 16;
		}

	}

	public static class MethodHandleParser extends ValueParser {

		@Override
		int parseNext(ConstantStore destination, byte[] raw, int startFrom) {
			destination.addConstant(new MethodHandle(destination, raw[startFrom],
					readIndex(raw, startFrom + 1)));
			return startFrom + 3;
		}

		@Override
		byte getTag() {
			return 15;
		}

	}

	public static class NameAndTypeParser extends ValueParser {

		@Override
		int parseNext(ConstantStore destination, byte[] raw, int startFrom) {
			destination.addConstant(new NameAndTypeImpl(destination, readIndex(raw, startFrom), readIndex(raw,
					startFrom + 2)));
			return startFrom + 4;
		}

		@Override
		byte getTag() {
			return 12;
		}

	}

	public static class InterfaceMethodReferenceParser extends ValueParser {

		@Override
		int parseNext(ConstantStore data, byte[] raw, int startFrom) {
			data.addConstant(new InterfaceMethodReferenceImpl(data, readIndex(raw, startFrom), readIndex(raw,
					startFrom + 2)));
			return startFrom + 4;
		}

		@Override
		byte getTag() {
			return 11;
		}

	}

	public static class MethodReferenceParser extends ValueParser {

		@Override
		int parseNext(ConstantStore data, byte[] raw, int startFrom) {
			data.addConstant(new MethodReferenceImpl(data, readIndex(raw,
					startFrom), readIndex(raw, startFrom + 2)));
			return startFrom + 4;
		}

		@Override
		byte getTag() {
			return 10;
		}

	}

	public static class FieldReferenceParser extends ValueParser {

		@Override
		int parseNext(ConstantStore data, byte[] raw, int startFrom) {
			data.addConstant(new FieldReferenceImpl(data, readIndex(raw,
					startFrom), readIndex(raw, startFrom + 2)));
			return startFrom + 4;
		}

		@Override
		byte getTag() {
			return 9;
		}

	}

	public static class StringReferenceParser extends ValueParser {

		@Override
		int parseNext(ConstantStore destination, byte[] raw, int startFrom) {
			destination.addConstant(new StringReferenceImpl(destination, readIndex(raw, startFrom)));
			return startFrom + 2;
		}

		@Override
		byte getTag() {
			return 8;
		}

	}

	public static class ClassConstParser extends ValueParser {

		@Override
		int parseNext(ConstantStore destination, byte[] raw, int startFrom) {
			destination.addConstant(new ClassReferenceImpl(destination, readIndex(raw, startFrom)));
			return startFrom + 2;
		}

		@Override
		byte getTag() {
			return 7;
		}

	}

	public static class DoubleParser extends LongParser {
		@Override
		byte getTag() {
			return 6;
		}

		@Override
		protected Number getValue(byte[] raw, int startFrom) {
			return Double.longBitsToDouble(super.getValue(raw, startFrom)
					.longValue());
		}

	}

	public static class LongParser extends ValueParser {

		@Override
		int parseNext(ConstantStore destination, byte[] raw, int startFrom) {
			destination.addConstant(getValue(raw, startFrom));
			return startFrom + 8;
		}

		protected Number getValue(byte[] raw, int startFrom) {

			return (((long) IntegerParser.parseInt(raw, startFrom)) << 32)
					+ IntegerParser.parseInt(raw, startFrom + 4);
		}

		@Override
		byte getTag() {
			return 5;
		}

	}

	public static class FloatParser extends IntegerParser {
		@Override
		byte getTag() {
			return 4;
		}

		@Override
		protected Float getValue(byte[] raw, int startFrom) {
			return Float.intBitsToFloat(super.getValue(raw, startFrom)
					.intValue());
		}

	}

	public static class IntegerParser extends ValueParser {

		@Override
		int parseNext(ConstantStore destination, byte[] raw, int startFrom) {
			destination.addConstant(getValue(raw, startFrom));
			return startFrom + 4;
		}

		protected Number getValue(byte[] raw, int startFrom) {

			return parseInt(raw, startFrom);
		}

		static int parseInt(byte[] raw, int startFrom) {
			return (raw[startFrom] & 0xFF) << 24
					| (raw[(startFrom + 1)] & 0xFF) << 16
					| (raw[(startFrom + 2)] & 0xFF) << 8 | raw[(startFrom + 3)]
					& 0xFF;
		}

		@Override
		byte getTag() {
			return 3;
		}

	}

	static class Utf8Parser extends ValueParser {

		@Override
		int parseNext(ConstantStore destination, byte[] raw, int startFrom) {
			int length = readIndex(raw, startFrom);// get string length from the
													// first 2 bytes
			destination.addConstant(parseUtf(startFrom + 2, length, raw));
			return startFrom + 2 + length;
		}
		
		private String parseUtf(int index, int length, byte[] raw) {
			return new String(raw, index, length);
		}

		@Override
		byte getTag() {
			return 1;
		}

	}

	static abstract class ValueParser {
		abstract int parseNext(ConstantStore destination, byte[] raw,
				int startFrom);

		abstract byte getTag();

		protected int readIndex(byte[] raw, int startFrom) {
			return ((raw[startFrom] & 0xFF) << 8 | raw[(startFrom + 1)] & 0xFF);
		}
	}

	public ConstantPool(final byte[] raw){
		this.raw=raw;
		parseConstantPool();
	}
	

	



	private void parseConstantPool() {
		this.addConstant("Index 0 is not used, constant pool is indexed from 1");
		int index = startFromIndex;
		while (index < raw.length && this.constantCount() < maxIndex ) {
			final ValueParser parser = parserForTag(raw[index]);
			index = parser.parseNext(this, raw, ++index);
		}
	}

	private static ValueParser parserForTag(final byte tag) {
		final ValueParser parser = parserMap.get(tag);
		if (parser == null) {
			throw new IllegalArgumentException("No parser for tag: " + tag);
		}
		return parser;

	}

	private static Map<Byte, ValueParser> buildParserMap(
			Collection<ValueParser> all) {

		HashMap<Byte, ValueParser> map = new HashMap<Byte, ConstantPool.ValueParser>();
		for (final ValueParser parser : all) {
			map.put(parser.getTag(), parser);
		}
		return map;
	}

	private List<Object> data=new ArrayList<Object>();

	public Object getConstant(int index) {
		return this.data.get(index);
	}






	public int constantCount() {
		return this.data.size();
	}






	public void addConstant(Object constant) {
		this.data.add(constant);
	}

}
