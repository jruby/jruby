package org.jruby.ir.persistence.parser;

import java.util.HashMap;
import java.util.Map;

import org.jruby.Ruby;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRScope;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Variable;

public class IRParsingContext {
    
    private Ruby runtime;
    private Map<String, IRScope> scopesByNames = new HashMap<String, IRScope>();
    private IRScope toplevelScope;
    private IRScope currentScope;
    private Map<String, Variable> variablesByNames = new HashMap<String, Variable>();
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
        scopesByNames.put(scope.getName(), scope);
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
    }
    
    public IRScope getToplevelScope() {
        return toplevelScope;
    }

    public Variable getVariablesByName(String name) {
        return variablesByNames.get(name);
    }

    public void addVariable(Variable variable) {
        this.variablesByNames.put(variable.getName(), variable);
    }

    public Label getLabel(String labelValue) {
        return labelsByNames.get(labelValue);      
    }    
    
    public void addLabel(Label label) {
        this.labelsByNames.put(label.label, label);      
    }  
    
}
