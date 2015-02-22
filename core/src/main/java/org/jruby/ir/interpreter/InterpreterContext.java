package org.jruby.ir.interpreter;

import org.jruby.ir.IRClassBody;
import org.jruby.ir.IREvalScript;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRMetaClassBody;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRModuleBody;
import org.jruby.ir.IRScope;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.representations.CFG;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;

public class InterpreterContext {
    private final int temporaryVariablecount;
    private final int temporaryBooleanVariablecount;
    private final int temporaryFixnumVariablecount;
    private final int temporaryFloatVariablecount;

    private final String name;
    private final String fileName;
    private final int lineNumber;
    private final StaticScope staticScope;
    private final Instr[] instructions;

    // Cached computed fields
    private final boolean hasExplicitCallProtocol;
    private final boolean isDynscopeEliminated;
    private final boolean pushNewDynScope;
    private final boolean reuseParentDynScope;
    private final boolean popDynScope;
    private final boolean receivesKeywordArguments;
    private final boolean metaClassBodyScope;

    private final static InterpreterEngine BODY_INTERPRETER = new BodyInterpreterEngine();
    private final static InterpreterEngine DEFAULT_INTERPRETER = new InterpreterEngine();
    private final static InterpreterEngine SIMPLE_METHOD_INTERPRETER = new InterpreterEngine();
    public final InterpreterEngine engine;

    // FIXME: Hack this should be a clone eventually since JIT might change this.  Comment for it reflects what it should be.
    // View of CFG at time of creating this context.
    private CFG cfg = null;

    private int runCount = 0;
    private boolean rebuilt = false;

    public InterpreterContext(IRScope scope, Instr[] instructions, boolean rebuild) {
        //FIXME: Remove once we conditionally plug in CFG on debug-only
        this.cfg = scope.getCFG();
        this.rebuilt = rebuild;
        if (this.rebuilt) {
            this.runCount = 30;
        }

/*
        if (scope instanceof IRModuleBody || scope instanceof IRClassBody) {
            engine = BODY_INTERPRETER;
        // ENEBO: Playing with unboxable and subset instruction sets
        //} else if (scope instanceof IRMethod && scope.getFlags().contains(IRFlags.SIMPLE_METHOD)) {
        //    engine = SIMPLE_METHOD_INTERPRETER;
        } else {
            engine = DEFAULT_INTERPRETER;
        }
*/
        engine = DEFAULT_INTERPRETER;

        this.name = scope.getName();
        this.fileName = scope.getFileName();
        this.lineNumber = scope.getLineNumber();
        this.staticScope = scope.getStaticScope();
        this.metaClassBodyScope = scope instanceof IRMetaClassBody;
        this.temporaryVariablecount = scope.getTemporaryVariablesCount();
        this.temporaryBooleanVariablecount = scope.getBooleanVariablesCount();
        this.temporaryFixnumVariablecount = scope.getFixnumVariablesCount();
        this.temporaryFloatVariablecount = scope.getFloatVariablesCount();
        this.instructions = instructions;
        this.hasExplicitCallProtocol = scope.getFlags().contains(IRFlags.HAS_EXPLICIT_CALL_PROTOCOL);
        this.reuseParentDynScope = scope.getFlags().contains(IRFlags.REUSE_PARENT_DYNSCOPE);
        this.isDynscopeEliminated = scope.getFlags().contains(IRFlags.DYNSCOPE_ELIMINATED);
        this.pushNewDynScope = !isDynscopeEliminated && !reuseParentDynScope;
        this.popDynScope = this.pushNewDynScope || this.reuseParentDynScope;
        this.receivesKeywordArguments = scope.getFlags().contains(IRFlags.RECEIVES_KEYWORD_ARGS);
    }

    public CFG getCFG() {
        return this.cfg;
    }

    public boolean isRebuilt() {
        return this.rebuilt;
    }

    public void incrementRunCount() {
        this.runCount++;
    }

    public boolean needsRebuilding() {
        return this.runCount == 30;
    }

    public Object[] allocateTemporaryVariables() {
        return temporaryVariablecount > 0 ? new Object[temporaryVariablecount] : null;
    }

    public boolean[] allocateTemporaryBooleanVariables() {
        return temporaryBooleanVariablecount > 0 ? new boolean[temporaryBooleanVariablecount] : null;
    }

    public long[] allocateTemporaryFixnumVariables() {
        return temporaryFixnumVariablecount > 0 ? new long[temporaryFixnumVariablecount] : null;
    }

    public double[] allocateTemporaryFloatVariables() {
        return temporaryFloatVariablecount > 0 ? new double[temporaryFloatVariablecount] : null;
    }

    public String getFileName() {
        return fileName;
    }

    public StaticScope getStaticScope() {
        return staticScope;
    }

    public int getTemporaryVariablecount() {
        return temporaryVariablecount;
    }

    public int getTemporaryBooleanVariablecount() {
        return temporaryBooleanVariablecount;
    }

    public int getTemporaryFixnumVariablecount() {
        return temporaryFixnumVariablecount;
    }

    public int getTemporaryFloatVariablecount() {
        return temporaryFloatVariablecount;
    }

    public Instr[] getInstructions() {
        return instructions;
    }

    public boolean isDynscopeEliminated() {
        return isDynscopeEliminated;
    }

    /**
     * Get a new dynamic scope.  Note: This only works for method scopes (ClosureIC will throw).
     */
    public DynamicScope newDynamicScope(ThreadContext context) {
        // Add a parent-link to current dynscope to support non-local returns cheaply. This doesn't
        // affect variable scoping since local variables will all have the right scope depth.
        if (metaClassBodyScope) return DynamicScope.newDynamicScope(staticScope, context.getCurrentScope());

        return DynamicScope.newDynamicScope(staticScope);
    }

    public boolean hasExplicitCallProtocol() {
        return hasExplicitCallProtocol;
    }

    public boolean pushNewDynScope() {
        return pushNewDynScope;
    }

    public boolean reuseParentDynScope() {
        return reuseParentDynScope;
    }

    public boolean popDynScope() {
        return popDynScope;
    }

    public boolean receivesKeywordArguments() {
        return receivesKeywordArguments;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append(fileName).append(':').append(lineNumber);
        if (name != null) buf.append(' ').append(name);

        if (cfg != null) {
            buf.append("\nCFG:\n").append(cfg.toStringInstrs());
        } else {
            int i = 0;
            for (Instr instr : instructions) {
                if (i > 0) buf.append("\n");
                buf.append("  ").append(i).append('\t').append(instr);
                i++;
            }
        }

        return buf.toString();
    }
}
