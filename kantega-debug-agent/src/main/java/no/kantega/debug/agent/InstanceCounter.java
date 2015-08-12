package no.kantega.debug.agent;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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

import org.slf4j.LoggerFactory;

import com.sun.jdi.ClassType;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;

/**
 * I provide a bridge from Instance informatino in JDI exposed as JMX attributes
 * 
 * @author marska
 *
 */
public class InstanceCounter implements DynamicMBean {
	private final VirtualMachine vm;
	private List<String> attributes = new LinkedList<String>();

	InstanceCounter(final VirtualMachine target) {
		this.vm = target;
	}

	@Override
	public Object getAttribute(final String className) {

		long[] counts = this.vm
				.instanceCounts(this.vm.classesByName(className));
		return sum(counts);
	}

	/**
	 * Sum instance counts
	 * 
	 * @param counts
	 * @return
	 */
	private long sum(long[] counts) {
		long sum = 0;
		for (long count : counts) {
			sum += count;
		}
		return sum;
	}

	@Override
	public AttributeList getAttributes(String[] attributes) {
		final AttributeList attributeList = new AttributeList(attributes.length);
		for (String attributeName : attributes) {
			attributeList.add(new Attribute(attributeName,
					getAttribute(attributeName)));
		}
		return attributeList;

	}

	@Override
	public MBeanInfo getMBeanInfo() {
		return new MBeanInfo("DebugInstanceCounter",
				"MBean bridge to instance counts in JDI",
				this.getAttributesInfo(), new MBeanConstructorInfo[0],
				getOperationInfo(), new MBeanNotificationInfo[0]);
	}

	private MBeanOperationInfo[] getOperationInfo() {

		return new MBeanOperationInfo[] {
				getMethod("addClass", String.class),
				getMethod("removeClass", String.class),
				getMethod("getLoadedClasses")};
	}

	private MBeanOperationInfo getMethod(String methodName, Class<?> ... classes ) {
		try {
			return new MBeanOperationInfo(methodName, this.getClass()
					.getDeclaredMethod(methodName, classes));
		} catch (Exception e) {
			throw new DebugAgentTechnicalRuntimeException("Unable to find method " + methodName, e);
		} 
	}

	public boolean addClass(final String className) {
		return this.attributes.add(className);
	}

	public boolean removeClass(final String className) {
		return this.attributes.remove(className);
	}
	
	public String[] getLoadedClasses() {
		List<String> classes=new LinkedList<String>();
		List<ReferenceType> loadedClasses = this.vm.allClasses();
		LoggerFactory.getLogger(this.getClass()).info("VM reports {} loaded classes", loadedClasses.size());
		for (ReferenceType referenceType : loadedClasses) {
			if(referenceType instanceof ClassType) {
				classes.add(referenceType.name());
			}
		}
		LoggerFactory.getLogger(this.getClass()).info("Of which {} are classes", loadedClasses.size());
		return classes.toArray(new String[classes.size()]);
	}

	private MBeanAttributeInfo[] getAttributesInfo() {
		LoggerFactory.getLogger(this.getClass()).info(
				"Buidling attribute list for " + this.getClass());
		if (!this.vm.canGetInstanceInfo()) {
			return new MBeanAttributeInfo[0];
		}
		final List<MBeanAttributeInfo> attributes = new ArrayList<MBeanAttributeInfo>(
				this.attributes.size());
		for (String className : this.attributes) {

				attributes.add(new MBeanAttributeInfo(className, "long", "Instances of " + className,
						true,
						false, false));

		}
		LoggerFactory.getLogger(this.getClass()).info(
				"Using " + attributes.size() + " attributes");

		return attributes.toArray(new MBeanAttributeInfo[attributes.size()]);
	}

	@Override
	public Object invoke(String actionName, Object[] params, String[] signature)
			throws MBeanException, ReflectionException {
		if("addClass".equals(actionName)) {
			return this.addClass(params[0].toString());
			
		}
		else if("removeClass".equals(actionName)) {
			return this.removeClass(params[0].toString());
		}
		else if("getLoadedClasses".equals(actionName)) {
			return this.getLoadedClasses();
		}
		else {
			throw new UnsupportedOperationException("Unknown operation " + actionName);
		}
	}

	@Override
	public void setAttribute(Attribute attribute)
			throws AttributeNotFoundException, InvalidAttributeValueException,
			MBeanException, ReflectionException {
		throw new UnsupportedOperationException("Can not set attributes");

	}

	@Override
	public AttributeList setAttributes(AttributeList attributes) {
		throw new UnsupportedOperationException("Can not set attributes");
	}

}
