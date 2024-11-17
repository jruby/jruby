package org.jruby.ir;

import org.jruby.EvalType;
import org.jruby.ir.operands.Label;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Signature;
import org.jruby.util.ByteList;

public class IREvalScript extends IRClosure {
    private String fileName;

    private static final ByteList EVAL = new ByteList(new byte[] {'E', 'V', 'A', 'L'});

    public IREvalScript(IRManager manager, IRScope lexicalParent, String fileName,
            int lineNumber, StaticScope staticScope, EvalType evalType) {
        // ALL eval scopes are not really closures and are always new scopes.  We should not reference an eval
        // by a TemporaryClosureVariable ever.
        super(manager, lexicalParent, lineNumber, staticScope, 0, EVAL);

        this.fileName = fileName;
        this.signature = Signature.NO_ARGUMENTS;

        if (staticScope != null) {
            // SSS FIXME: This is awkward!
            if (evalType == EvalType.MODULE_EVAL) {
                staticScope.setScopeType(getScopeType());
            } else {
                IRScope s = lexicalParent;
                while (s instanceof IREvalScript) {
                    s = s.getLexicalParent();
                }
                staticScope.setScopeType(s.getScopeType());
            }
        }
    }

    @Override
    public int getNextClosureId() {
        nextClosureIndex++;

        return nextClosureIndex;
    }

    @Override
    public Label getNewLabel() {
        return getNewLabel("EV" + closureId + "_LBL");
    }

    @Override
    public IRScopeType getScopeType() {
        return IRScopeType.EVAL_SCRIPT;
    }

    public boolean isWhereFlipFlopStateVariableIs() {
        return true;
    }

    @Override
    public boolean isScriptScope() {
        return true;
    }

    @Override
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String getFile() {
        return fileName;
    }
}
