package no.kantega.debug.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import no.kantega.debug.bytecode.ConstantPool;
import no.kantega.debug.execution.model.Expression;

import com.sun.xml.internal.ws.org.objectweb.asm.Opcodes;

public class DebugExpressionResolver {

	static class MethodInvocation extends ExpressionBuilder {

		@Override
		public Expression expressionFor(ConstantPool constantPool,
				byte[] methodCodes, int currentIndex) {
			return 	(Expression) constantPool.getConstant(methodCodes[currentIndex+2]);
		}

		@Override
		public byte[] supportsByteCodes() {
			
			return new byte[]{(byte)Opcodes.INVOKEVIRTUAL, (byte)Opcodes.INVOKEINTERFACE};
		}

	}




	static Collection<ExpressionBuilder> allBuilders = Arrays.asList((ExpressionBuilder)new MethodInvocation());
	static Map<Byte, ExpressionBuilder> expressionBuilders = builderMap(allBuilders);

	static abstract class ExpressionBuilder {

		public abstract Expression expressionFor(ConstantPool constantPool,
				byte[] methodCodes, int currentIndex);
		
		public abstract byte[] supportsByteCodes();

	}


 
	
	public static Expression expressionAtLocation(com.sun.jdi.Location location) {
		byte[] methodCodes = location.method().bytecodes();
		int codeIndex = (int) location.codeIndex();//move one message send back
		ConstantPool constantPool = new ConstantPool(location.declaringType().constantPool());
		return expressionBuilders.get(methodCodes[codeIndex]).expressionFor(constantPool, methodCodes, codeIndex);
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




	private static Map<Byte, ExpressionBuilder> builderMap(
			Collection<ExpressionBuilder> builders) {
		final Map<Byte, ExpressionBuilder> builderMap = new HashMap<Byte, DebugExpressionResolver.ExpressionBuilder>();
		for(ExpressionBuilder builder : builders){
			for(byte code : builder.supportsByteCodes()){
				builderMap.put(code, builder);
			}
		}
		return builderMap;
	}

}
