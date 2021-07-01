package org.jruby.ir.interpreter;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;

import org.jruby.RubySymbol;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRMetaClassBody;
import org.jruby.ir.IRScope;
import org.jruby.ir.instructions.ExceptionRegionEndMarkerInstr;
import org.jruby.ir.instructions.ExceptionRegionStartMarkerInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.LabelInstr;
import org.jruby.ir.representations.CFG;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;

public class InterpreterContext {

    private final static Instr[] NO_INSTRUCTIONS = new Instr[0];

    private final static InterpreterEngine DEFAULT_INTERPRETER = new InterpreterEngine();
    private final static InterpreterEngine STARTUP_INTERPRETER = new StartupInterpreterEngine();

    protected int temporaryVariableCount;

    // startup interp will mark this at construction and not change but full interpreter will write it
    // much later after running compiler passes.  JIT will not use this field at all.
    protected Instr[] instructions;

    // Contains pairs of values.  The first value is number of instrs in this range + number of instrs before
    // this range.  The second number is the rescuePC.  getRescuePC(ipc) will walk this list and first odd value
    // less than this value will be the rpc.
    protected int[] rescueIPCs = null;

    // Cached computed fields
    protected boolean hasExplicitCallProtocol; // Only can be true in Full+
    protected boolean dynamicScopeEliminated; // Only can be true in Full+
    private boolean reuseParentDynScope; // Only can be true in Full+
    private boolean metaClassBodyScope;

    private InterpreterEngine engine;
    public final Supplier<List<Instr>> instructionsCallback;
    private EnumSet<IRFlags> flags;

    private final IRScope scope;

    public InterpreterContext(IRScope scope, List<Instr> instructions, int temporaryVariableCount, EnumSet<IRFlags> flags) {
        this.scope = scope;

        // FIXME: Hack null instructions means coming from FullInterpreterContext but this should be way cleaner
        // For impl testing - engine = determineInterpreterEngine(scope);
        this.engine = instructions == null ? DEFAULT_INTERPRETER : STARTUP_INTERPRETER;

        this.metaClassBodyScope = scope instanceof IRMetaClassBody;
        setInstructions(instructions);
        this.instructionsCallback = null; // engine != null
        this.temporaryVariableCount = temporaryVariableCount;
        this.flags = flags;
    }

    public InterpreterContext(IRScope scope, Supplier<List<Instr>> instructions, int temporaryVariableCount, EnumSet<IRFlags> flags) {
        this.scope = scope;

        this.metaClassBodyScope = scope instanceof IRMetaClassBody;
        this.instructionsCallback = instructions;
        this.temporaryVariableCount = temporaryVariableCount;
        this.flags = flags;
    }

    protected void initialize() {
        if (instructions == null) getEngine();
    }

    public InterpreterEngine getEngine() {
        if (engine == null) {
            setInstructions(instructionsCallback.get());

            // FIXME: Hack null instructions means coming from FullInterpreterContext but this should be way cleaner
            // For impl testing - engine = determineInterpreterEngine(scope);
            this.engine = instructions == null ? DEFAULT_INTERPRETER : STARTUP_INTERPRETER;
        }
        return engine;
    }

    public Instr[] getInstructions() {
        initialize();

        return instructions == null ? NO_INSTRUCTIONS : instructions;
    }

    private void setInstructions(final List<Instr> instructions) {
        this.instructions = instructions != null ? prepareBuildInstructions(instructions) : null;
    }

    private Instr[] prepareBuildInstructions(List<Instr> instructions) {
        int length = instructions.size();
        Instr[] linearizedInstrArray = instructions.toArray(new Instr[length]);

        for (int ipc = 0; ipc < length; ipc++) {
            Instr i = linearizedInstrArray[ipc];

            if (i instanceof LabelInstr) ((LabelInstr) i).getLabel().setTargetPC(ipc + 1);
        }

        Deque<Integer> markers = new ArrayDeque<>(8);
        rescueIPCs = new int[length];
        int rpc = -1;

        for (int ipc = 0; ipc < length; ipc++) {
            Instr i = linearizedInstrArray[ipc];

            if (i instanceof ExceptionRegionStartMarkerInstr) {
                rpc = ((ExceptionRegionStartMarkerInstr) i).getFirstRescueBlockLabel().getTargetPC();
                markers.push(rpc);
            } else if (i instanceof ExceptionRegionEndMarkerInstr) {
                markers.pop();
                rpc = markers.isEmpty() ? -1 : markers.peek().intValue();
            }

            rescueIPCs[ipc] = rpc;
        }

        return linearizedInstrArray;
    }

    public int[] getRescueIPCs() {
        return rescueIPCs;
    }

    public int getRequiredArgsCount() {
        return getStaticScope().getSignature().required();
    }

    public IRScope getScope() {
        return scope;
    }

    public CFG getCFG() {
        return null;
    }

    public int getTemporaryVariableCount() {
        return temporaryVariableCount;
    }

    public Object[] allocateTemporaryVariables() {
        return temporaryVariableCount > 0 ? new Object[temporaryVariableCount] : null;
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
        return scope.getFile();
    }

    public RubySymbol getName() {
        return scope.getManager().getRuntime().newSymbol(scope.getId());
    }

    public void computeScopeFlagsFromInstructions() {
        for (Instr instr : getInstructions()) {
            instr.computeScopeFlags(scope, getFlags());
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
        initialize();

        return hasExplicitCallProtocol;
    }

    public void setExplicitCallProtocol(boolean callProtocol) {
        this.hasExplicitCallProtocol = callProtocol;
    }

    public boolean isDynamicScopeEliminated() {
        return dynamicScopeEliminated;
    }

    public void setDynamicScopeEliminated(boolean dynamicScopeEliminated) {
        this.dynamicScopeEliminated = dynamicScopeEliminated;
    }

    public boolean pushNewDynScope() {
        initialize();

        return !dynamicScopeEliminated && !reuseParentDynScope;
    }

    public boolean reuseParentDynScope() {
        initialize();

        return reuseParentDynScope;
    }

    public void setReuseParentDynScope(boolean reuseParentDynScope) {
        this.reuseParentDynScope = reuseParentDynScope;
    }

    public boolean popDynScope() {
        initialize();

        return pushNewDynScope() || this.reuseParentDynScope();
    }

    public boolean receivesKeywordArguments() {
        return scope.receivesKeywordArgs();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append(getFileName()).append(':').append(scope.getLine());
        if (getName() != null) buf.append(' ').append(getName()).append("\n");

        if (getInstructions() == null) {
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
            b.append(String.format("%6d",i)).append('\t').append(instructions[i]);
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

    public EnumSet<IRFlags> getFlags() {
        return flags;
    }
}
