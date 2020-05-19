package org.jruby.ir;

import org.jruby.RubySymbol;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;

public class IRScriptBody extends IRScope {
    private DynamicScope toplevelScope;
    private String fileName;

    public IRScriptBody(IRManager manager, String sourceName, StaticScope staticScope) {
        super(manager, null, null, 0, staticScope);
        this.toplevelScope = null;
        this.fileName = sourceName;

        if (staticScope != null) {
            staticScope.setIRScope(this);
        }
    }

    public DynamicScope getScriptDynamicScope() {
        return toplevelScope;
    }

    public void setScriptDynamicScope(DynamicScope tlbScope) {
        this.toplevelScope = tlbScope;
    }

    @Override
    public int getNearestModuleReferencingScopeDepth() {
        return 0;
    }

    @Override
    public IRScopeType getScopeType() {
        return IRScopeType.SCRIPT_BODY;
    }

    @Override
    public String toString() {
        return "Script: file: " + getFile() + super.toString();
    }

    @Override
    public boolean isScriptScope() {
        return true;
    }

    @Override
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFile() {
        return fileName;
    }

    public String getId() {
        return fileName;
    }

    public RubySymbol getName() {
        return getManager().getRuntime().newSymbol(fileName);
    }

    @Override
    public void cleanupAfterExecution() {
        if (getClosures().isEmpty()) {
            interpreterContext = null;
            fullInterpreterContext = null;
            localVars = null;
        }
    }
}
