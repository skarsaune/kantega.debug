package com.sun.org.apache.bcel.internal.classfile;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import no.kantega.debug.bytecode.JniTypeToSourceTranslator;

import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;

public class MethodToJavaLangReflectMethod {
	static Map<String, ObjectReference> nameToType;

	private static Map<String, ObjectReference> nameToType(VirtualMachine vm) {
		if (nameToType == null) {
			nameToType = new HashMap<String, ObjectReference>();
			addPrimitiveToCache(nameToType, Boolean.class, Boolean.TYPE, vm);
			addPrimitiveToCache(nameToType, Integer.class, Integer.TYPE, vm);
			addPrimitiveToCache(nameToType, Long.class, Long.TYPE, vm);
			addPrimitiveToCache(nameToType, Byte.class, Byte.TYPE, vm);
			addPrimitiveToCache(nameToType, Short.class, Short.TYPE, vm);
			addPrimitiveToCache(nameToType, Character.class, Character.TYPE, vm);
			addPrimitiveToCache(nameToType, Double.class, Double.TYPE, vm);
			addPrimitiveToCache(nameToType, Float.class, Float.TYPE, vm);
		}
		return nameToType;
	}

	private static void addPrimitiveToCache(Map<String, ObjectReference> cache,
			Class<?> type, Class<?> primitiveClass, VirtualMachine vm)  {
		final String qualifiedName = type.getName();

		
		List<ReferenceType> classesByName = vm.classesByName(qualifiedName);
		if(classesByName.isEmpty()) {
			return;
		}
		final ReferenceType referenceType = classesByName
				.get(0);
		ClassType classType = (ClassType) referenceType.getValue(
				referenceType.fieldByName("TYPE")).type();
		cache.put(primitiveClass.getName(), classType.classObject());

	}
	
	/**
	 * 
	 * @param klass class
	 * @param vm target vm
	 * @return reference to the class object representing the class in the target vm
	 */
	public static ObjectReference classObjectFor(final String klass, final VirtualMachine vm) {
		ObjectReference objectReference = nameToType(vm).get(klass);
		if(objectReference == null) {
			objectReference = vm.classesByName(klass).get(0).classObject();
		}
		return objectReference;
	}
	
	
	public static ObjectReference javaLangReflectMethodFor(final com.sun.jdi.Method method, final com.sun.jdi.ThreadReference executeIn) throws InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException, InvocationException {
		final ObjectReference declaringClass = classObjectFor(method.declaringType().name(), method.virtualMachine());
		List<ObjectReference> paramTypesList=new LinkedList<ObjectReference>();
		paramTypesList.add(method.virtualMachine().mirrorOf(method.name()));
		List<String> types = JniTypeToSourceTranslator.javaTypesFromJni(method.signature());
		for (int i = 0; i < types.size() - 1; i++) {
			paramTypesList.add(classObjectFor(types.get(i), method.virtualMachine()));
		}
		executeIn.suspend();
		return (ObjectReference) declaringClass.invokeMethod(executeIn, getDeclaredMethodMethod(method.virtualMachine()), paramTypesList, ObjectReference.INVOKE_SINGLE_THREADED);
	}

	private static Method getDeclaredMethodMethod(VirtualMachine vm) {
		
		return ((ReferenceType) classObjectFor(Class.class.getName(), vm).type()).methodsByName("getDeclaredMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;").get(0);
	}
	
	
}
