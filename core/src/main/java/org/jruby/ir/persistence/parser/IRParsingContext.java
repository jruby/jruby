package org.jruby.ir.persistence.parser;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jruby.Ruby;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRScope;

public enum IRParsingContext {
    INSTANCE;
    
    // FIXME: Or store them as tread context?
    private Ruby runtime;
    private Map<String, IRScope> scopesByNames = new HashMap<String, IRScope>();
    private IRScope currentScope;
    
    public void newFileStarted() {
        scopesByNames.clear();
        currentScope = null;
    }
    
    public void setRuntime(Ruby runtime) {
        this.runtime =runtime;        
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
        this.currentScope = currentScope;
    }
    
    public Collection<IRScope> getScopes() {
        return scopesByNames.values();
    }

}
