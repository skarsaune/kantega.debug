package no.kantega.debug.util.test;

import java.util.Collections;

import no.kantega.debug.decompile.ClassFileReverseEnginerer;
import no.kantega.debug.decompile.JdiLazyLoader;
import no.kantega.debug.decompile.JdiVarNamesCollector;
import no.kantega.debug.util.DebugExpressionResolver;

import org.hamcrest.core.Is;
import org.jetbrains.java.decompiler.main.ClassesProcessor;
import org.jetbrains.java.decompiler.main.ClassesProcessor.ClassNode;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.collectors.CounterContainer;
import org.jetbrains.java.decompiler.main.collectors.ImportCollector;
import org.jetbrains.java.decompiler.main.rels.MethodProcessorRunnable;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarProcessor;
import org.jetbrains.java.decompiler.struct.StructContext;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.struct.gen.MethodDescriptor;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;

public class DebugExpressionResolverTest {
	
	@Mock Location location;
	@Mock com.sun.jdi.Method method;
	@Mock com.sun.jdi.ReferenceType type;
	@Mock LocalVariable variable;

	@Test
	public void testExpressionResolution() throws Exception {
		MockitoAnnotations.initMocks(this);
		Mockito.when(location.codeIndex()).thenReturn(11l);
		Mockito.when(location.method()).thenReturn(this.method);
		byte [] methodByteCodes = {-78, 0, 17, 18, 23, -74, 0, 25, 1, 76, 43, -74, 0, 31, -74, 0, 35, -74, 0, 35, -74, 0, 35, 87, -79};
		Mockito.when(method.bytecodes()).thenReturn(methodByteCodes);
		Mockito.when(method.name()).thenReturn("test");
		Mockito.when(method.signature()).thenReturn("()V");
		Mockito.when(method.modifiers()).thenReturn(0);
		Mockito.when(method.declaringType()).thenReturn(this.type);
		Mockito.when(method.variables()).thenReturn(Collections.singletonList(this.variable));
		Mockito.when(this.variable.name()).thenReturn("a");
		Mockito.when(this.variable.typeName()).thenReturn("java/lang/Object");
		Mockito.when(location.declaringType()).thenReturn(this.type);
		byte[] pool={7, 0, 2, 1, 0, 19, 100, 101, 98, 117, 103, 116, 101, 115, 116, 47, 68, 101, 98, 117, 103, 84, 101, 115, 116, 7, 0, 4, 1, 0, 16, 106, 97, 118, 97, 47, 108, 97, 110, 103, 47, 79, 98, 106, 101, 99, 116, 1, 0, 6, 60, 105, 110, 105, 116, 62, 1, 0, 3, 40, 41, 86, 1, 0, 4, 67, 111, 100, 101, 10, 0, 3, 0, 9, 12, 0, 5, 0, 6, 1, 0, 15, 76, 105, 110, 101, 78, 117, 109, 98, 101, 114, 84, 97, 98, 108, 101, 1, 0, 18, 76, 111, 99, 97, 108, 86, 97, 114, 105, 97, 98, 108, 101, 84, 97, 98, 108, 101, 1, 0, 4, 116, 104, 105, 115, 1, 0, 21, 76, 100, 101, 98, 117, 103, 116, 101, 115, 116, 47, 68, 101, 98, 117, 103, 84, 101, 115, 116, 59, 1, 0, 4, 116, 101, 115, 116, 1, 0, 25, 82, 117, 110, 116, 105, 109, 101, 86, 105, 115, 105, 98, 108, 101, 65, 110, 110, 111, 116, 97, 116, 105, 111, 110, 115, 1, 0, 16, 76, 111, 114, 103, 47, 106, 117, 110, 105, 116, 47, 84, 101, 115, 116, 59, 9, 0, 18, 0, 20, 7, 0, 19, 1, 0, 16, 106, 97, 118, 97, 47, 108, 97, 110, 103, 47, 83, 121, 115, 116, 101, 109, 12, 0, 21, 0, 22, 1, 0, 3, 111, 117, 116, 1, 0, 21, 76, 106, 97, 118, 97, 47, 105, 111, 47, 80, 114, 105, 110, 116, 83, 116, 114, 101, 97, 109, 59, 8, 0, 24, 1, 0, 11, 72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100, 10, 0, 26, 0, 28, 7, 0, 27, 1, 0, 19, 106, 97, 118, 97, 47, 105, 111, 47, 80, 114, 105, 110, 116, 83, 116, 114, 101, 97, 109, 12, 0, 29, 0, 30, 1, 0, 7, 112, 114, 105, 110, 116, 108, 110, 1, 0, 21, 40, 76, 106, 97, 118, 97, 47, 108, 97, 110, 103, 47, 83, 116, 114, 105, 110, 103, 59, 41, 86, 10, 0, 3, 0, 32, 12, 0, 33, 0, 34, 1, 0, 8, 116, 111, 83, 116, 114, 105, 110, 103, 1, 0, 20, 40, 41, 76, 106, 97, 118, 97, 47, 108, 97, 110, 103, 47, 83, 116, 114, 105, 110, 103, 59, 10, 0, 36, 0, 32, 7, 0, 37, 1, 0, 16, 106, 97, 118, 97, 47, 108, 97, 110, 103, 47, 83, 116, 114, 105, 110, 103, 1, 0, 1, 97, 1, 0, 18, 76, 106, 97, 118, 97, 47, 108, 97, 110, 103, 47, 79, 98, 106, 101, 99, 116, 59, 1, 0, 10, 83, 111, 117, 114, 99, 101, 70, 105, 108, 101, 1, 0, 14, 68, 101, 98, 117, 103, 84, 101, 115, 116, 46, 106, 97, 118, 97};
		Mockito.when(type.constantPool()).thenReturn(pool);
		Mockito.when(type.constantPoolCount()).thenReturn(42);
		Mockito.when(type.minorVersion()).thenReturn(0);
		Mockito.when(type.majorVersion()).thenReturn(52);
		Mockito.when(type.name()).thenReturn("debugtest/DebugTest");
		Mockito.when(type.methods()).thenReturn(Collections.singletonList(method));
		final String expression = DebugExpressionResolver.expressionAtLocation(this.location);
		Assert.assertThat(expression, Is.is("java.lang.Object.toString() : java.lang.String"));
		StructMethod reverseEngineered = ClassFileReverseEnginerer.reverseEngineer(method);
		reverseEngineered.expandData();
		DecompilerContext.setCounterContainer(new CounterContainer());
		DecompilerContext.setProperty(DecompilerContext.CURRENT_METHOD_DESCRIPTOR, MethodDescriptor.parseDescriptor(method.signature()));
		DecompilerContext.setProperty(DecompilerContext.CURRENT_METHOD, reverseEngineered);
		DecompilerContext.setProperty(DecompilerContext.CURRENT_CLASS, reverseEngineered.getClassStruct());
		DecompilerContext.setVarNamesCollector(new JdiVarNamesCollector(this.method));
		StructContext context = new StructContext(null, null, new JdiLazyLoader(type));
		DecompilerContext.setStructContext(context);
		DecompilerContext.setClassProcessor(new ClassesProcessor(context));
		DecompilerContext.setImportCollector(new ImportCollector(new ClassNode(ClassNode.CLASS_ROOT, reverseEngineered.getClassStruct())));
		System.out.println(MethodProcessorRunnable.codeToJava(reverseEngineered, new VarProcessor()).toJava().getOriginalText().toString());
	}

}
