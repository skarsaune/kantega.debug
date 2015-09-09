package no.kantega.debug.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import no.kantega.debug.bytecode.ConstantPool;
import no.kantega.debug.execution.model.Expression;

import com.sun.jdi.Location;

public class DebugExpressionResolver {

	static class MethodInvocation extends ExpressionBuilder {

		@Override
		public Expression expressionFor(ConstantPool constantPool,
				byte[] methodCodes, int currentIndex) {
			return 	(Expression) constantPool.getConstant(methodCodes[currentIndex+2]);
		}

		@Override
		public byte[] supportsByteCodes() {
			
			return new byte[]{-71, -74};
		}

	}




	static Collection<ExpressionBuilder> allBuilders = Arrays.asList((ExpressionBuilder)new MethodInvocation());
	static Map<Byte, ExpressionBuilder> expressionParsers = builderMap(allBuilders);

	static abstract class ExpressionBuilder {

		public abstract Expression expressionFor(ConstantPool constantPool,
				byte[] methodCodes, int currentIndex);
		
		public abstract byte[] supportsByteCodes();

	}


 
	
	public static Expression expressionAtLocation(final Location location) {
		final byte[] methodCodes = location.method().bytecodes();
		final int codeIndex = (int) location.codeIndex();
		final byte currentByteCode = methodCodes[codeIndex];
		final byte[] poolBytes = location.declaringType().constantPool();
		final ConstantPool constantPool = new ConstantPool(poolBytes);
		return expressionParsers.get(currentByteCode).
				expressionFor(constantPool, methodCodes, codeIndex);
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
