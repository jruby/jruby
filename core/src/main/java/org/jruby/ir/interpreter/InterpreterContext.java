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
    protected int temporaryVariablecount;

    // startup interp will mark this at construction and not change but full interpreter will write it
    // much later after running compiler passes.  JIT will not use this field at all.
    protected Instr[] instructions;

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

    private IRScope scope;

    public InterpreterContext(IRScope scope, List<Instr> instructions) {
        this.scope = scope;

        // FIXME: Hack null instructions means coming from FullInterpreterContext but this should be way cleaner
        // For impl testing - engine = determineInterpreterEngine(scope);
        engine = instructions == null ? DEFAULT_INTERPRETER : STARTUP_INTERPRETER;

        this.metaClassBodyScope = scope instanceof IRMetaClassBody;
        this.temporaryVariablecount = scope.getTemporaryVariablesCount();
        this.instructions = instructions != null ? prepareBuildInstructions(instructions) : null;
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

    public int getRequiredArgsCount() {
        return getStaticScope().getSignature().required();
    }

    public IRScope getScope() {
        return scope;
    }

    /**
     * Is the build complete?  For startup builds, which this class represents, we finish build in the constructor
     * so it is always complete.  For FullInterpreterContext this is more complicated (see javadocs there for more
     * info).
     */
    public boolean buildComplete() {
        return true;
    }

    public CFG getCFG() {
        return null;
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

    public void computeScopeFlagsFromInstructions() {
        for (Instr instr : getInstructions()) {
            instr.computeScopeFlags(scope);
        }
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
        if (getName() != null) buf.append(' ').append(getName()).append("\n");

        if (instructions == null) {
            buf.append("  No Instructions.  Full Build before linearizeInstr?");
        } else {
            buf.append(toStringInstrs()).append("\n");
        }

        return buf.toString();
    }

    public String toStringInstrs() {
        StringBuilder b = new StringBuilder();
        int length = instructions.length;

        for (int i = 0; i < length; i++) {
            if (i > 0) b.append("\n");
            b.append("  ").append(i).append('\t').append(instructions[i]);
        }

        /* ENEBO: I this this is too much output espectially for ic and not fic
        Collection<IRClosure> nestedClosures = scope.getClosures();
        if (nestedClosures != null && !nestedClosures.isEmpty()) {
            b.append("\n\n------ Closures encountered in this scope ------\n");
            for (IRClosure c: nestedClosures)
                b.append(c.toStringBody());
            b.append("------------------------------------------------\n");
        }*/

        return b.toString();
    }
}