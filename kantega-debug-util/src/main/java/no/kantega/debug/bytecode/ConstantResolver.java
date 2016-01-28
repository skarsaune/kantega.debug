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
	
	/**
	 * @param representation String representation of the constant we want the index of
	 *@return Get the constant pool index of the constant, -1 if not found 
	 */
	int indexOfConstantPrintedAs(String representation);
}
