package no.kantega.debug.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;

import no.kantega.debug.bytecode.JniTypeToSourceTranslator;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.struct.consts.ConstantPool;
import org.jetbrains.java.decompiler.struct.consts.LinkConstant;

import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;

public class DebugExpressionResolver {

//	static class MethodInvocation extends ExpressionBuilder {
//
//		@Override
//		public Expression expressionFor(ConstantPool constantPool,
//				byte[] methodCodes, int currentIndex) {
//			return 	(Expression) constantPool.getConstant(methodCodes[currentIndex+2]);
//		}
//
//		@Override
//		public byte[] supportsByteCodes() {
//			
//			return new byte[]{-71, -74};
//		}
//
//	}
//
//
//
//
//	static Collection<ExpressionBuilder> allBuilders = Arrays.asList((ExpressionBuilder)new MethodInvocation());
//	static Map<Byte, ExpressionBuilder> expressionParsers = builderMap(allBuilders);
//
//	static abstract class ExpressionBuilder {
//
//		public abstract Object expressionFor(ConstantPool constantPool,
//				byte[] methodCodes, int currentIndex);
//		
//		public abstract byte[] supportsByteCodes();
//
//	}


 
	
	public static String expressionAtLocation(final Location location) throws IOException {
		final byte[] methodCodes = location.method().bytecodes();
		final int codeIndex = (int) location.codeIndex();
		final DataInputStream byteCodeReader = new DataInputStream(new ByteArrayInputStream(methodCodes, codeIndex, 3));
		final int currentByteCode = byteCodeReader.read();
		if(isMethodInvocation(currentByteCode)) {//should always be true!
			final ConstantPool constantPool = parseConstantPool(location.declaringType());
			final int methodReference = byteCodeReader.readUnsignedShort();
			//decompiler constant pool representation of method invocation
			final LinkConstant constant = (LinkConstant) constantPool.getConstant(methodReference);
			return printConstant(constant);
		} else {
			return null;
		}
	}

	private static boolean isMethodInvocation(final int currentByteCode) {
		return currentByteCode == CodeConstants.opc_invokeinterface || currentByteCode ==
		CodeConstants.opc_invokevirtual;
	}

	private static String printConstant(final LinkConstant constant) {
		return constant.classname.replace('/', '.') + "." + constant.elementname + JniTypeToSourceTranslator.javaSignatureFromJni(constant.descriptor);
	}
	
	@SuppressWarnings("unchecked")
	private static org.jetbrains.java.decompiler.struct.consts.ConstantPool parseConstantPool(final ReferenceType type)
			throws IOException {
		DecompilerContext.initContext(Collections.EMPTY_MAP);
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




//	private static int previousMethodInvocation(long codeIndex, byte[] methodCodes) {
//		int nextCode = (int) codeIndex;
//		for(int i=nextCode-1; i>0; i-- ) {
//			byte previousByte=methodCodes[i];
//			if(previousByte == -71 || previousByte == -74) {
//				return i;
//			}
//		}
//		return -1;
//	}



//	private static Map<Byte, ExpressionBuilder> builderMap(
//			Collection<ExpressionBuilder> builders) {
//		final Map<Byte, ExpressionBuilder> builderMap = new HashMap<Byte, DebugExpressionResolver.ExpressionBuilder>();
//		for(ExpressionBuilder builder : builders){
//			for(byte code : builder.supportsByteCodes()){
//				builderMap.put(code, builder);
//			}
//		}
//		return builderMap;
//	}

}
