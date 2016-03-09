package no.kantega.debug.agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ReflectionException;

import no.kantega.debug.memory.DebugMemoryAccessor;

import org.slf4j.LoggerFactory;

import com.sun.jdi.ClassType;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;

/**
 * I provide a bridge from Instance information in JDI exposed as JMX attributes
 * 
 * @author marska
 *
 */
public class InstanceCounter implements DynamicMBean {
	private VirtualMachine vm;
	private Collection<String> attributes = new TreeSet<String>();

	public static Collection<String> leakCandidates = Arrays.asList(
			"java.sql.Connection", "java.sql.ResultSet", "java.sql.DataSource",
			"java.sql.Statement");

	void setVirtualMachine(final VirtualMachine vm) {
		this.vm = vm;
	}

	public Object getAttribute(final String className) {

		if (cannotAccessMemory()) {
			return -1L;
		}
		try {
			return DebugMemoryAccessor.instances(this.vm, className);
		} catch (VMDisconnectedException e) {
			this.vm = null;
			return -1L;
		}
	}

	public AttributeList getAttributes(String[] attributes) {
		final AttributeList attributeList = new AttributeList(attributes.length);
		for (String attributeName : attributes) {
			attributeList.add(new Attribute(attributeName,
					getAttribute(attributeName)));
		}
		return attributeList;

	}

	public MBeanInfo getMBeanInfo() {
		return new MBeanInfo("DebugInstanceCounter",
				"MBean bridge to instance counts in JDI",
				this.getAttributesInfo(), new MBeanConstructorInfo[0],
				getOperationInfo(), new MBeanNotificationInfo[0]);
	}

	public Collection<String> monitoredClasses() {
		return this.attributes;
	}

	public boolean enabled() {
		return !this.cannotAccessMemory();
	}

	private MBeanOperationInfo[] getOperationInfo() {

		return new MBeanOperationInfo[] { getMethod("addClass", String.class),
				getMethod("removeClass", String.class),
				getMethod("monitoredClasses"), getMethod("enabled") };
	}

	private MBeanOperationInfo getMethod(String methodName, Class<?>... classes) {
		try {
			return new MBeanOperationInfo(methodName, this.getClass()
					.getDeclaredMethod(methodName, classes));
		} catch (Exception e) {
			throw new DebugAgentTechnicalRuntimeException(
					"Unable to find method " + methodName, e);
		}
	}

	public boolean addClass(final String className) {
		if (!this.cannotAccessMemory()
				&& this.vm.classesByName(className).isEmpty()) {
			throw new IllegalArgumentException(className
					+ " is not loaded in this VM");
		}
		boolean result = this.attributes.add(className);
		return result;
	}

	public boolean removeClass(final String className) {
		return this.attributes.remove(className);
	}

	public String[] getLoadedClasses() {
		if (cannotAccessMemory()) {
			return new String[] { "Unable to list classes" };
		}
		Collection<String> classes = loadedClassesFromJdi();
		return classes.toArray(new String[classes.size()]);
	}

	private Collection<String> loadedClassesFromJdi() {
		Collection<String> classes = new TreeSet<String>();
		List<ReferenceType> loadedClasses = this.vm.allClasses();
		LoggerFactory.getLogger(this.getClass()).debug(
				"VM reports {} loaded classes", loadedClasses.size());
		for (ReferenceType referenceType : loadedClasses) {
			if (referenceType instanceof ClassType) {
				classes.add(referenceType.name());
			}
		}
		LoggerFactory.getLogger(this.getClass()).debug(
				"Of which {} are classes", loadedClasses.size());
		return classes;
	}

	public Collection<String> candidateClassesForFilter(final String filter) {
		if (filter.length() < 3 || cannotAccessMemory()) {
			return Arrays.asList(filter);
		} else {
			final String upperCase = filter.toUpperCase();
			Collection<String> candidates = new TreeSet<String>();
			for (String aClass : loadedClassesFromJdi()) {
				if (aClass.toUpperCase().contains(upperCase)) {
					candidates.add(aClass);
				}
			}
			return candidates;
		}
	}

	private MBeanAttributeInfo[] getAttributesInfo() {
		LoggerFactory.getLogger(this.getClass()).trace(
				"Buidling attribute list for " + this.getClass());
		if (cannotAccessMemory()) {
			return new MBeanAttributeInfo[0];
		}
		final List<MBeanAttributeInfo> attributes = new ArrayList<MBeanAttributeInfo>(
				this.attributes.size());
		for (String className : this.attributes) {

			attributes.add(new MBeanAttributeInfo(className, "long",
					"Instances of " + className, true, false, false));

		}
		LoggerFactory.getLogger(this.getClass()).trace(
				"Using " + attributes.size() + " attributes");

		return attributes.toArray(new MBeanAttributeInfo[attributes.size()]);
	}

	private boolean cannotAccessMemory() {
		return this.vm == null || !this.vm.canGetInstanceInfo();
	}

	public Object invoke(String actionName, Object[] params, String[] signature)
			throws MBeanException, ReflectionException {
		if ("addClass".equals(actionName)) {
			return this.addClass(params[0].toString());

		} else if ("removeClass".equals(actionName)) {
			return this.removeClass(params[0].toString());
		} else if ("monitoredClasses".equals(actionName)) {
			return this.monitoredClasses();
		} else {
			throw new UnsupportedOperationException("Unknown operation "
					+ actionName);
		}
	}

	public void setAttribute(Attribute attribute)
			throws AttributeNotFoundException, InvalidAttributeValueException,
			MBeanException, ReflectionException {
		throw new UnsupportedOperationException("Can not set attributes");

	}

	public AttributeList setAttributes(AttributeList attributes) {
		throw new UnsupportedOperationException("Can not set attributes");
	}

	public void setMonitoredClasses(List<String> classes) {
		this.attributes.clear();
		this.attributes.addAll(classes);

	}

	public List<String> getResourceLeakCandidates() {
		List<String> candidates = new LinkedList<String>();
		if (this.vm != null) {
			for (final String resource : leakCandidates) {
				if (!this.vm.classesByName(resource).isEmpty()) {
					candidates.add(resource);
				}
			}
		}
		return candidates;

	}

	public Map<String, Long> implementorsAndCounts(final String resource) {
		if(this.vm == null || !this.vm.canGetInstanceInfo()) {
			return Collections.emptyMap();
		} else {
			return DebugMemoryAccessor.implementorsAndCounts(resource, this.vm);
		}
	}



}
