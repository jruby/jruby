package org.jruby.ir.persistence.read.parser;

import java.util.HashMap;
import java.util.Map;

import org.jruby.Ruby;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRScope;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.TemporaryVariable;

public class IRParsingContext {
    
    private Ruby runtime;
    private Map<String, IRScope> scopesByNames = new HashMap<String, IRScope>();
    private IRScope toplevelScope;
    private IRScope currentScope;
    private Map<String, TemporaryVariable> variablesByNames = new HashMap<String, TemporaryVariable>();
    private Map<String, Label> labelsByNames = new HashMap<String, Label>();
    
    public IRParsingContext(Ruby runtime) {
        this.runtime = runtime;
    }
    
    public Ruby getRuntime() {
        return runtime;
    }
    
    public IRManager getIRManager() {
        return runtime.getIRManager();
    }
    
    public void addToScopes(IRScope scope) {
        scopesByNames.put(scope.getName()+":"+scope.getLineNumber(), scope);
    }
    
    public IRScope getScopeByName(String name) {
        return scopesByNames.get(name);
    }

    public IRScope getCurrentScope() {
        return currentScope;
    }

    public void setCurrentScope(IRScope currentScope) {
        if(toplevelScope == null) {
            this.toplevelScope = currentScope;
        }
        this.currentScope = currentScope;
        labelsByNames.clear();
        variablesByNames.clear();
    }
    
    public IRScope getToplevelScope() {
        return toplevelScope;
    }
    
    public boolean isContainsVariable(String name) {
        return variablesByNames.containsKey(name);
    }

    public TemporaryVariable getVariable(String name) {
        return variablesByNames.get(name);
    }

    public void addVariable(TemporaryVariable variable) {
        this.variablesByNames.put(variable.getName(), variable);
    }
    
    public boolean isContainsLabel(String labelValue) {
        return labelsByNames.containsKey(labelValue);
    }

    public Label getLabel(String labelValue) {
        return labelsByNames.get(labelValue);      
    }
    
    /**
     * @param labelName name of label in original IR
     * @param label created during the IR reading
     */
    public void addLabel(String labelName, Label label) {
        this.labelsByNames.put(labelName, label);      
    }
    
}
