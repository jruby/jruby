package org.jruby.ir;

import org.jruby.EvalType;
import org.jruby.ir.operands.Label;
import org.jruby.parser.StaticScope;
import org.jruby.util.ByteList;

public class IREvalScript extends IRClosure {
    private String fileName;

    private static final ByteList EVAL_ = new ByteList(new byte[] {'E', 'V', 'A', 'L', '_'});

    public IREvalScript(IRManager manager, IRScope lexicalParent, String fileName,
            int lineNumber, StaticScope staticScope, EvalType evalType) {
        super(manager, lexicalParent, lineNumber, staticScope, EVAL_);

        this.fileName = fileName;

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
    public Label getNewLabel() {
        return getNewLabel("EV" + closureId + "_LBL");
    }

    @Override
    public IRScopeType getScopeType() {
        return IRScopeType.EVAL_SCRIPT;
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
