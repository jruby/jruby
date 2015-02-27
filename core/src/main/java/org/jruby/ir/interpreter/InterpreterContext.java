package org.jruby.ir.interpreter;

import java.util.List;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRMetaClassBody;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRModuleBody;
import org.jruby.ir.IRScope;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.LabelInstr;
import org.jruby.ir.representations.CFG;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;

public class InterpreterContext {
    private final int temporaryVariablecount;

    private final Instr[] instructions;

    // Cached computed fields
    private final boolean hasExplicitCallProtocol;
    private final boolean pushNewDynScope;
    private final boolean reuseParentDynScope;
    private final boolean popDynScope;
    private final boolean receivesKeywordArguments;
    private final boolean metaClassBodyScope;

    private final static InterpreterEngine BODY_INTERPRETER = new BodyInterpreterEngine();
    private final static InterpreterEngine DEFAULT_INTERPRETER = new InterpreterEngine();
    private final static InterpreterEngine STARTUP_INTERPRETER = new StartupInterpreterEngine();
    private final static InterpreterEngine SIMPLE_METHOD_INTERPRETER = new InterpreterEngine();
    public final InterpreterEngine engine;

    private int runCount = 0;
    private boolean rebuilt = false;
    private IRScope scope;

    public InterpreterContext(IRScope scope, List<Instr> instructions) {
        if (this.rebuilt) {
            this.runCount = 30;
        }

        this.scope = scope;
        // For impl testing - engine = determineInterpreterEngine(scope);
        engine = STARTUP_INTERPRETER;

        this.metaClassBodyScope = scope instanceof IRMetaClassBody;
        this.temporaryVariablecount = scope.getTemporaryVariablesCount();
        this.instructions = prepareBuildInstructions(instructions);
        this.hasExplicitCallProtocol = scope.getFlags().contains(IRFlags.HAS_EXPLICIT_CALL_PROTOCOL);
        this.reuseParentDynScope = scope.getFlags().contains(IRFlags.REUSE_PARENT_DYNSCOPE);
        this.pushNewDynScope = !scope.getFlags().contains(IRFlags.DYNSCOPE_ELIMINATED) && !reuseParentDynScope;
        this.popDynScope = this.pushNewDynScope || this.reuseParentDynScope;
        this.receivesKeywordArguments = scope.getFlags().contains(IRFlags.RECEIVES_KEYWORD_ARGS);
    }

    private InterpreterEngine determineInterpreterEngine(IRScope scope) {
        if (scope instanceof IRModuleBody) {
            return BODY_INTERPRETER;
        } else if (scope instanceof IRMethod && scope.getFlags().contains(IRFlags.SIMPLE_METHOD)) {
            return SIMPLE_METHOD_INTERPRETER; // ENEBO: Playing with unboxable and subset instruction sets
        } else {
            return DEFAULT_INTERPRETER;
        }
    }

    private Instr[] prepareBuildInstructions(List<Instr> instructions) {
        int length = instructions.size();
        Instr[] linearizedInstrArray = instructions.toArray(new Instr[length]);
        for (int ipc = 0; ipc < length; ipc++) {
            Instr i = linearizedInstrArray[ipc];
            i.setIPC(ipc);

            if (i instanceof LabelInstr) ((LabelInstr) i).getLabel().setTargetPC(ipc + 1);
        }

        return linearizedInstrArray;
    }

    public IRScope getScope() {
        return scope;
    }
    public CFG getCFG() {
        return null;
    }

    public boolean isRebuilt() {
        return rebuilt;
    }

    public void incrementRunCount() {
        //runCount++;
    }

    public boolean needsRebuilding() {
        return runCount >= 30;
    }

    public Object[] allocateTemporaryVariables() {
        return temporaryVariablecount > 0 ? new Object[temporaryVariablecount] : null;
    }

    public boolean[] allocateTemporaryBooleanVariables() {
        return null;
    }

    public long[] allocateTemporaryFixnumVariables() {
        return null;
    }

    public double[] allocateTemporaryFloatVariables() {
        return null;
    }

    public StaticScope getStaticScope() {
        return scope.getStaticScope();
    }

    public String getFileName() {
        return scope.getFileName();
    }

    public String getName() {
        return scope.getName();
    }

    public Instr[] getInstructions() {
        return instructions;
    }

    /**
     * Get a new dynamic scope.  Note: This only works for method scopes (ClosureIC will throw).
     */
    public DynamicScope newDynamicScope(ThreadContext context) {
        // Add a parent-link to current dynscope to support non-local returns cheaply. This doesn't
        // affect variable scoping since local variables will all have the right scope depth.
        if (metaClassBodyScope) return DynamicScope.newDynamicScope(getStaticScope(), context.getCurrentScope());

        return DynamicScope.newDynamicScope(getStaticScope());
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

        buf.append(getFileName()).append(':').append(scope.getLineNumber());
        if (getName() != null) buf.append(' ').append(getName());

        int i = 0;
        for (Instr instr : instructions) {
            if (i > 0) buf.append("\n");
            buf.append("  ").append(i).append('\t').append(instr);
            i++;
        }

        return buf.toString();
    }
}