package no.kantega.debug.decompile;

import java.lang.reflect.Field;

import com.sun.jdi.Mirror;

public class JdiReflectionHacks {
	
	@SuppressWarnings("unused")
	private static final int privateFieldToCheckReflectionAccess = 3;
	
	private static final boolean hasPrivateReflectionAccess = hasPrivateReflectionAccess();
	
	private static boolean hasPrivateReflectionAccess() {
		try {
			Field field = JdiReflectionHacks.class.getDeclaredField("privateFieldToCheckReflectionAccess");
			field.setAccessible(true);
			field.get(null);
			return true;
		} catch (Exception ignore) {
			return false;
		}
	}
	
	static boolean isSunJdi(Mirror probe) {
		return probe.getClass().getPackage().getName().startsWith("com.sun.tools.jdi");
	}
	
	static boolean isEclipseJdi(Mirror probe) {
		return probe.getClass().getPackage().getName().startsWith("org.eclipse.jdi.internal");
	}
	
	public static boolean canRetrievePrivateInfo(Mirror probe) {
		return hasPrivateReflectionAccess && (isSunJdi(probe) || isEclipseJdi(probe));
	}
	
	static Object readField(final Object object, final String field) throws ReflectiveOperationException {
		 Field fieldref = object.getClass().getDeclaredField(field);
		 fieldref.setAccessible(true);
		 return fieldref.get(object);
	}

}
