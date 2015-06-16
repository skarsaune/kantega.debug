package no.kantega.debug.bytecode;

/**
 * Ability to resolve and return constant pool values
 * @author marska
 *
 */
public interface ConstantResolver {
	/**
	 * @param index - index of constant , note: constants are indexed from 1 
	 *@return Constant value at given index 
	 */
	Object getConstant(int index);
	
	/**
	 * 
	 * @return Total number of constants (up to highest index)
	 */
	int constantCount();
}
