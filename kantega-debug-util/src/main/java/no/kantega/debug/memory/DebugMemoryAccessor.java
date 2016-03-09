package no.kantega.debug.memory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.sun.jdi.ClassType;
import com.sun.jdi.InterfaceType;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;

/**
 * I provide convenience methods for accessing type and memory information from a VM
 * @author marska
 *
 */
public class DebugMemoryAccessor {
	
	
	/**
	 * @param vm A connected Vitual machine that suppors traversing the heap
	 * @param className fully qualified java Class name
	 * @return The global instance count of classes named className
	 */
	public static long instances(final VirtualMachine vm,
			String className) {
		List<ReferenceType> classesOfName = vm.classesByName(className);
		return sumInstancesOfClasses(vm, classesOfName);
	}

	private static long sumInstancesOfClasses(final VirtualMachine vm,
			List<? extends ReferenceType> classesOfName) {
		return sum(vm.instanceCounts(classesOfName));
	}

	/**
	 * Sum instance counts
	 * 
	 * @param counts
	 * @return
	 */
	private static long sum(long[] counts) {
		long sum = 0;
		for (long count : counts) {
			sum += count;
		}
		return sum;
	}
	
	
	/**
	 * 
	 * @param hierarchyRoot - root of the type hierarchy that we are interrested in
	 * @param vm - virtual machine that must support heap traversal
	 * @return Map of class names and the number of instances
	 */
	public static Map<String, Long> implementorsAndCounts(final String hierarchyRoot, final VirtualMachine vm) {
		Map<String, Long> result = new TreeMap<String, Long>();
		if (vm != null) {
			for (final ReferenceType resourceRoot : vm
					.classesByName(hierarchyRoot)) {
				if (resourceRoot instanceof ClassType) {
					addImplementors(((ClassType) resourceRoot).subclasses(),
							result, vm);
				} else if (resourceRoot instanceof InterfaceType) {
					addImplementors(
							((InterfaceType) resourceRoot).implementors(),
							result, vm);
				}
			}
		}
		return result;
	}

	private static void addImplementors(final List<ClassType> implementors,
			final Map<String, Long> result, final VirtualMachine vm) {
		for (ClassType classType : implementors) {
			if(!classType.isAbstract()) { 
				result.put(classType.name(), sumInstancesOfClasses(vm, Collections.singletonList(classType)));
			}
			addImplementors(classType.subclasses(), result, vm);
		}
	}

	


}
