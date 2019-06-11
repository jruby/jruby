package org.jruby.ir;

import org.jruby.RubySymbol;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;

public class IRScriptBody extends IRScope {
    private DynamicScope toplevelScope;
    // FIXME: bytelist_love - This is pretty weird...look at how we use getId in this and consider alternative.
    private RubySymbol fileName;

    public IRScriptBody(IRManager manager, RubySymbol sourceName, StaticScope staticScope) {
        super(manager, null, sourceName, 0, staticScope);
        this.toplevelScope = null;
        this.fileName = sourceName;

        if (!getManager().isDryRun() && staticScope != null) {
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
        return "Script: file: " + getFileName() + super.toString();
    }

    @Override
    public boolean isScriptScope() {
        return true;
    }

    @Override
    public void setFileName(String fileName) {
        this.fileName = getManager().runtime.newSymbol(fileName);
    }

    public String getFile() {
        return fileName.asJavaString();
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
