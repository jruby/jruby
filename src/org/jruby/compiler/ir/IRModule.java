package org.jruby.compiler.ir;

import java.util.ArrayList;
import java.util.List;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;
import org.jruby.compiler.ir.operands.LocalVariable;
import org.jruby.parser.StaticScope;

public class IRModule extends IRScope {

    private CodeVersion version;    // Current code version for this module

    // Modules, classes, and methods that belong to this scope 
    //
    // LEXICAL scoping, but when a class, method, module definition is
    // encountered in a closure or a method in Ruby code, that definition
    // is pushed up to the nearest containing module!
    //
    // In most cases, this lexical scoping also matches actual class/module hierarchies
    // SSS FIXME: An example where they might be different?
    private List<IRModule> modules = new ArrayList<IRModule>();
    private List<IRClass> classes = new ArrayList<IRClass>();
    private List<IRMethod> methods = new ArrayList<IRMethod>();
    
    public IRModule(IRScope lexicalParent, String name, StaticScope scope) {
        super(lexicalParent, name, scope);
        
        updateVersion();
    }

    public List<IRModule> getModules() {
        return modules;
    }

    public List<IRClass> getClasses() {
        return classes;
    }

    public List<IRMethod> getMethods() {
        return methods;
    }

    public void addModule(IRModule m) {
        modules.add(m);
    }

    public void addClass(IRClass c) {
        classes.add(c);
    }

    public void addMethod(IRMethod method) {
        assert !method.isScriptBody();

        methods.add(method);
    }

    @Override
    public void runCompilerPassOnNestedScopes(CompilerPass p) {
        for (IRScope m : modules) {
            m.runCompilerPass(p);
        }

        for (IRScope c : classes) {
            c.runCompilerPass(p);
        }

        for (IRScope meth : methods) {
            meth.runCompilerPass(p);
        }
    }

    @Override
    public IRModule getNearestModule() {
        return this;
    }

    public void updateVersion() {
        version = CodeVersion.getClassVersionToken();
    }

    public String getScopeName() {
        return "Module";
    }

    public CodeVersion getVersion() {
        return version;
    }

    public IRMethod getInstanceMethod(String name) {
        for (IRMethod m : methods) {
            if (m.isInstanceMethod && m.getName().equals(name)) return m;
        }

        return null;
    }

    public LocalVariable getLocalVariable(String name, int scopeDepth) {
        LocalVariable lvar = findExistingLocalVariable(name);
        if (lvar == null) {
            lvar = new LocalVariable(name, scopeDepth, localVars.nextSlot);
            localVars.putVariable(name, lvar);
        }

        return lvar;
    }

    @Override
    public LocalVariable getImplicitBlockArg() {
        assert false: "A Script body never accepts block args";
        
        return null;
    }

    @Override
    public LocalVariable findExistingLocalVariable(String name) {
        return localVars.getVariable(name);
    }
    
    @Override
    public boolean isScriptBody() {
        return true;
    }
}
