package org.jruby.ir;

import org.jruby.EvalType;
import org.jruby.ir.operands.Label;
import org.jruby.parser.StaticScope;
import org.jruby.anno.CRuby;
import org.jruby.util.ByteList;

public class IREvalScript extends IRClosure {
    private String fileName;
    private final boolean fileNameSynthetic;

    private static final ByteList EVAL = new ByteList(new byte[] {'E', 'V', 'A', 'L'});

    // the start of the "(eval at FILE:LINE)" filename eval uses when none is given
    private static final String EVAL_LOCATION_MARK = "(eval at ";

    public IREvalScript(IRManager manager, IRScope lexicalParent, String fileName,
            int lineNumber, StaticScope staticScope, EvalType evalType) {
        this(manager, lexicalParent, fileName, lineNumber, staticScope, evalType, false);
    }

    public IREvalScript(IRManager manager, IRScope lexicalParent, String fileName,
            int lineNumber, StaticScope staticScope, EvalType evalType, boolean fileNameSynthetic) {
        // ALL eval scopes are not really closures and are always new scopes.  We should not reference an eval
        // by a TemporaryClosureVariable ever.
        super(manager, lexicalParent, lineNumber, staticScope, 0, EVAL);

        this.fileName = fileName;
        this.fileNameSynthetic = fileNameSynthetic;

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

    public boolean isFileNameSynthetic() {
        return fileNameSynthetic;
    }

    /**
     * Whether code in this eval has no path for require_relative and __dir__ to use, as CRuby's
     * rb_current_realfilepath decides. That's when eval made the filename up, or when it looks like
     * the "(eval at FILE:LINE)" name eval makes up, which CRuby recognizes by its shape. A filename
     * of "(eval)" given explicitly is a real path in CRuby, since only the one eval makes up counts.
     *
     * @return true if there is no path
     */
    @CRuby(value = "rb_current_realfilepath", file = "vm_eval.c")
    public boolean hasNoRealPath() {
        if (fileNameSynthetic) return true;

        String name = fileName;
        return name != null && name.length() > EVAL_LOCATION_MARK.length() + 1 &&
                name.startsWith(EVAL_LOCATION_MARK) && name.endsWith(")");
    }
}
