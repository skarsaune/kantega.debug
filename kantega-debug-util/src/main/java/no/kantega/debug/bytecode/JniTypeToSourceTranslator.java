package no.kantega.debug.bytecode;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * I parse and convert type names from JNI names to Java source names
 * 
 * @author marska
 *
 */
public class JniTypeToSourceTranslator {

	static Map<String, String> jniToPrim = buildJniToPrim();

	private static Map<String, String> buildJniToPrim() {

		final HashMap<String, String> mappings = new HashMap<String, String>();
		mappings.put("Z", "boolean");
		mappings.put("B", "byte");
		mappings.put("C", "char");
		mappings.put("S", "short");
		mappings.put("I", "int");
		mappings.put("J", "long");
		mappings.put("F", "float");
		mappings.put("D", "double");
		return mappings;
	}

	public static List<String> javaTypesFromJni(final String jniString) {
		LinkedList<String> types = new LinkedList<String>();
		for (int i = 0; i < jniString.length(); i++) {
			final char currentChar = jniString.charAt(i);
			final String mapped = jniToPrim.get(currentChar);
			if (mapped != null) {
				types.add(mapped);
			} else if (currentChar == 'L') {
				final int semi = jniString.indexOf(';', i);
				types.add(jniString.substring(i + 1, semi).replace('/', '.'));
				i = semi;
			} else if (currentChar == '(' || currentChar == ')') {

			}
		}
		return types;
	}

	public static String javaSignatureFromJni(final String jniString) {
		final List<String> types = javaTypesFromJni(jniString);
		final StringBuilder builder = new StringBuilder();
		builder.append('(');
		boolean firstParam = true;

		for (int i = 0; i < types.size() - 1; i++) {
			if (firstParam) {
				firstParam = false;
			} else {
				builder.append(" ,");
			}
			builder.append(types.get(i));
		}
		if (types.size() > 0) {
			builder.append(") : ");
			builder.append(types.get(types.size() - 1));
		}
		return builder.toString();

	}

}
