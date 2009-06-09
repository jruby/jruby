package org.jruby.buildr.ir;

public class IR_Method implements IR_BuilderContext
{
	String         _name;		// Ruby name 
	String         _irName;		// Generated name
	List<IR_Instr> _instrs;		// List of ir instructions for this method

	private HashMap<String, Integer> _nextVarIndex;

	public void IR_Method(String name, boolean isRoot)
	{
		_name = name;
    	if (root && Boolean.getBoolean("jruby.compile.toplevel")) {
            _irName = name;
        } else {
            String mangledName = JavaNameMangler.mangleStringForCleanJavaIdentifier(name);
			// FIXME: What is this script business here?
            _irName = "method__" + script.getAndIncrementMethodIndex() + "$RUBY$" + mangledName;
        }

		_nextVarIndex = new HashMap<String, Integer>();
	}

	public Variable getNewVariable(String prefix)
	{
		Integer idx = _nextVarIndex.get(prefix);
		if (idx == null)
			idx = 0;
		_nextVarIndex.put(prefix, idx+1);
		return new Variable(prefix + idx);
	}

	public Label getNewLabel()
	{
		Integer idx = _nextVarIndex.get("LBL_");
		if (idx == null)
			idx = 0;
		_nextVarIndex.put(prefix, idx+1);
		return new Label(prefix + idx);
	}

	public void addInstr(IR_Instr i) { _instrs.append(i); }
}
