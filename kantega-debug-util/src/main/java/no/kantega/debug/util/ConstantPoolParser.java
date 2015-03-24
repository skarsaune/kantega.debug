package no.kantega.debug.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Standalone utility without external dependencies to parse a constantPool byte
 * array returned from com.sun.jdi.ReferenceType.constantPool based on JVM
 * specification of class file format, for instance:
 * http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html
 * 
 * @author marska
 *
 */
public class ConstantPoolParser {

	public static class InvokeDynamicImpl extends ConstantPoolStructure{

		private int nameAndTypeIndex;
		private int bootstrapIndex;

		public InvokeDynamicImpl(List<Object> data, int bootstrapIndex , int nameAndTypeIndex) {
			super(data);
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

		public MethodTypeImpl(List<Object> data, int index) {
			super(data, index);
		}

	}

	public static class FieldReferenceImpl extends MemberReferenceImpl {

		public FieldReferenceImpl(final List<Object> data,
				final int classIndex, final int nameAndTypeIndex) {
			super(data, classIndex, nameAndTypeIndex);
		}

	}

	public static class MethodReferenceImpl extends MemberReferenceImpl {

		public MethodReferenceImpl(final List<Object> data,
				final int classIndex, final int nameAndTypeIndex) {
			super(data, classIndex, nameAndTypeIndex);
		}

	}

	public static class MemberReferenceImpl extends ConstantPoolStructure {

		private int classIndex;
		private int nameAndTypeIndex;

		public MemberReferenceImpl(List<Object> data, int classIndex,
				int nameAndTypeIndex) {
			super(data);
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

	}

	public static class InterfaceMethodReferenceImpl extends
			MemberReferenceImpl {

		public InterfaceMethodReferenceImpl(final List<Object> data,
				final int classIndex, final int nameAndTypeIndex) {
			super(data, classIndex, nameAndTypeIndex);
		}

	}

	public static class ClassReferenceImpl extends StringReferenceImpl {

		public ClassReferenceImpl(List<Object> data, int index) {
			super(data, index);
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

		public MethodHandle(List<Object> data, byte type, int index) {
			super(data);
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

		private List<Object> data;

		public ConstantPoolStructure(List<Object> data) {
			this.data = data;
		}

		protected Object getReference(final int index) {
			return this.data.get(index-1);
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

		public NameAndTypeImpl(List<Object> data, int nameIndex,
				int descriptorIndex) {
			super(data);
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

		public StringReferenceImpl(List<Object> destination, int index) {
			super(destination);
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
			new StringReferenceParser(), new ClassConstParser());
	private static Map<Byte, ValueParser> parserMap = buildParserMap(allParsers);

	public static class InvokeDynamicParser extends ValueParser {

		@Override
		int parseNext(List<Object> data, byte[] raw, int startFrom) {
			data.add(new InvokeDynamicImpl(data, readIndex(raw,
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
		int parseNext(List<Object> destination, byte[] raw, int startFrom) {
			destination.add(new MethodTypeImpl(destination, readIndex(raw, startFrom)));
			return startFrom + 2;
		}

		@Override
		byte getTag() {
			return 16;
		}

	}

	public static class MethodHandleParser extends ValueParser {

		@Override
		int parseNext(List<Object> destination, byte[] raw, int startFrom) {
			destination.add(new MethodHandle(destination, raw[startFrom],
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
		int parseNext(List<Object> destination, byte[] raw, int startFrom) {
			destination.add(new NameAndTypeImpl(destination, readIndex(raw, startFrom), readIndex(raw,
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
		int parseNext(List<Object> data, byte[] raw, int startFrom) {
			data.add(new InterfaceMethodReferenceImpl(data, readIndex(raw, startFrom), readIndex(raw,
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
		int parseNext(List<Object> data, byte[] raw, int startFrom) {
			data.add(new MethodReferenceImpl(data, readIndex(raw,
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
		int parseNext(List<Object> data, byte[] raw, int startFrom) {
			data.add(new FieldReferenceImpl(data, readIndex(raw,
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
		int parseNext(List<Object> destination, byte[] raw, int startFrom) {
			destination.add(new StringReferenceImpl(destination, readIndex(raw, startFrom)));
			return startFrom + 2;
		}

		@Override
		byte getTag() {
			return 8;
		}

	}

	public static class ClassConstParser extends ValueParser {

		@Override
		int parseNext(List<Object> destination, byte[] raw, int startFrom) {
			destination.add(new ClassReferenceImpl(destination, readIndex(raw, startFrom)));
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
		int parseNext(List<Object> destination, byte[] raw, int startFrom) {
			destination.add(getValue(raw, startFrom));
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
		int parseNext(List<Object> destination, byte[] raw, int startFrom) {
			destination.add(getValue(raw, startFrom));
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
		int parseNext(List<Object> destination, byte[] raw, int startFrom) {
			int length = readIndex(raw, startFrom);// get string length from the
													// first 2 bytes
			destination.add(parseUtf(startFrom + 2, length, raw));
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
		abstract int parseNext(List<Object> destination, byte[] raw,
				int startFrom);

		abstract byte getTag();

		protected int readIndex(byte[] raw, int startFrom) {
			return ((raw[startFrom] & 0xFF) << 8 | raw[(startFrom + 1)] & 0xFF);
		}
	}

	public static List<Object> parseConstantPool(byte[] raw) {

		return parseConstantPool(0, raw);
	}

	public static List<Object> parseConstantPool(final int startFrom, byte[] raw) {
		return parseConstantPool(startFrom, raw, Integer.MAX_VALUE);//do not limit items
	}

	public static List<Object> parseConstantPool(final int startFrom, byte[] raw, int toConstantIndex) {
		final List<Object> result = new ArrayList<Object>();
		int index = startFrom;
		while (index < raw.length && result.size() < toConstantIndex - 1) {
			final ValueParser parser = parserForTag(raw[index]);
			index = parser.parseNext(result, raw, ++index);
		}
		return result;
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

		HashMap<Byte, ValueParser> map = new HashMap<Byte, ConstantPoolParser.ValueParser>();
		for (final ValueParser parser : all) {
			map.put(parser.getTag(), parser);
		}
		return map;
	}

}
