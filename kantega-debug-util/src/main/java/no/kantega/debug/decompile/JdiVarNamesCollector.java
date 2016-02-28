package no.kantega.debug.decompile;

import java.util.List;

import org.jetbrains.java.decompiler.main.collectors.VarNamesCollector;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Method;

public class JdiVarNamesCollector extends VarNamesCollector {

	private Method method;

	public JdiVarNamesCollector(final Method method) {
		super();
		this.method = method;
	}
	
	@Override
	public String getFreeName(int index) {
		List<LocalVariable> variables=null;
		try {
			variables = this.method.variables();
		} catch (AbsentInformationException e) {
		}
		if(variables != null && index <= variables.size()) {
			if(!this.method.isStatic()) {
				if(index == 0) {
					return "this";
				}
				else {
					return variables.get(index -1).name();
				}
			}
			return variables.get(index).name();
		} else {
			return super.getFreeName(index);			
		}
		
	}

}
