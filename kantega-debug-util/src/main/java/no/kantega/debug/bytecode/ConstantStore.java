package no.kantega.debug.bytecode;

/**
 * A store for constant values read from constant pool
 * @author marska
 *
 */
public interface ConstantStore extends ConstantResolver {
	void addConstant(Object constant);
}
