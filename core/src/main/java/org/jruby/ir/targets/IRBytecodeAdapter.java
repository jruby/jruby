/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir.targets;

import com.headius.invokebinder.Signature;
import org.jruby.RubyModule;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.ir.instructions.ClosureAcceptingInstr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.targets.indy.IndyArgumentsCompiler;
import org.jruby.ir.targets.indy.IndyBlockCompiler;
import org.jruby.ir.targets.indy.IndyBranchCompiler;
import org.jruby.ir.targets.indy.IndyCheckpointCompiler;
import org.jruby.ir.targets.indy.IndyConstantCompiler;
import org.jruby.ir.targets.indy.IndyDynamicValueCompiler;
import org.jruby.ir.targets.indy.IndyGlobalVariableCompiler;
import org.jruby.ir.targets.indy.IndyInstanceVariableCompiler;
import org.jruby.ir.targets.indy.IndyInvocationCompiler;
import org.jruby.ir.targets.indy.IndyValueCompiler;
import org.jruby.ir.targets.indy.IndyYieldCompiler;
import org.jruby.ir.targets.simple.NormalArgumentsCompiler;
import org.jruby.ir.targets.simple.NormalBlockCompiler;
import org.jruby.ir.targets.simple.NormalBranchCompiler;
import org.jruby.ir.targets.simple.NormalCheckpointCompiler;
import org.jruby.ir.targets.simple.NormalConstantCompiler;
import org.jruby.ir.targets.simple.NormalDynamicValueCompiler;
import org.jruby.ir.targets.simple.NormalGlobalVariableCompiler;
import org.jruby.ir.targets.simple.NormalInstanceVariableCompiler;
import org.jruby.ir.targets.simple.NormalInvocationCompiler;
import org.jruby.ir.targets.simple.NormalValueCompiler;
import org.jruby.ir.targets.simple.NormalYieldCompiler;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Binding;
import org.jruby.runtime.Block;
import org.jruby.runtime.Frame;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.JavaNameMangler;
import org.jruby.util.collections.IntHashMap;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import java.lang.invoke.MethodType;

import static org.jruby.util.CodegenUtils.*;

/**
 *
 * @author headius
 */
public class IRBytecodeAdapter {
    public static final int MAX_ARGUMENTS = 250;

    public IRBytecodeAdapter(BytecodeMode bytecodeMode, SkinnyMethodAdapter adapter, Signature signature, ClassData classData) {
        this.adapter = adapter;
        this.signature = signature;
        this.classData = classData;

        switch (bytecodeMode) {
            case INDY:
                this.valueCompiler = new IndyValueCompiler(this);
                this.dynamicValueCompiler = new IndyDynamicValueCompiler(this);
                this.invocationCompiler = new IndyInvocationCompiler(this);
                this.branchCompiler = new IndyBranchCompiler(this);
                this.checkpointCompiler = new IndyCheckpointCompiler(this);
                this.instanceVariableCompiler = new IndyInstanceVariableCompiler(this);
                this.globalVariableCompiler = new IndyGlobalVariableCompiler(this);
                this.yieldCompiler = new IndyYieldCompiler(this);
                this.blockCompiler = new IndyBlockCompiler(this);
                this.argumentsCompiler = new IndyArgumentsCompiler(this);
                this.constantCompiler = new IndyConstantCompiler(this);
                break;
            case MIXED:
                this.valueCompiler = new IndyValueCompiler(this);
                this.dynamicValueCompiler = new NormalDynamicValueCompiler(this);
                this.invocationCompiler = new NormalInvocationCompiler(this);
                this.branchCompiler = new NormalBranchCompiler(this);
                this.checkpointCompiler = new NormalCheckpointCompiler(this);
                this.instanceVariableCompiler = new NormalInstanceVariableCompiler(this);
                this.globalVariableCompiler = new IndyGlobalVariableCompiler(this);
                this.yieldCompiler = new IndyYieldCompiler(this);
                this.blockCompiler = new IndyBlockCompiler(this);
                this.argumentsCompiler = new NormalArgumentsCompiler(this);
                this.constantCompiler = new IndyConstantCompiler(this);
                break;
            case AOT:
                this.valueCompiler = new NormalValueCompiler(this);
                this.dynamicValueCompiler = new NormalDynamicValueCompiler(this);
                this.invocationCompiler = new NormalInvocationCompiler(this);
                this.branchCompiler = new NormalBranchCompiler(this);
                this.checkpointCompiler = new NormalCheckpointCompiler(this);
                this.instanceVariableCompiler = new NormalInstanceVariableCompiler(this);
                this.globalVariableCompiler = new NormalGlobalVariableCompiler(this);
                this.yieldCompiler = new NormalYieldCompiler(this);
                this.blockCompiler = new NormalBlockCompiler(this);
                this.argumentsCompiler = new NormalArgumentsCompiler(this);
                this.constantCompiler = new NormalConstantCompiler(this);
                break;
            default:
                throw new RuntimeException("unknown compile mode: " + bytecodeMode);
        }
    }

    /**
     * Get the compiler for constant Ruby values.
     *
     * @return the value compiler
     */
    public ValueCompiler getValueCompiler() {
        return valueCompiler;
    }

    /**
     * Get the compiler for dynamic values.
     *
     * @return the dynamic value compiler
     */
    public DynamicValueCompiler getDynamicValueCompiler() {
        return dynamicValueCompiler;
    }

    /**
     * Get the compiler for invocations.
     *
     * @return the invocation compiler
     */
    public InvocationCompiler getInvocationCompiler() {
        return invocationCompiler;
    }

    /**
     * Get the compiler for dynamic branches.
     *
     * @return the branch compiler
     */
    public BranchCompiler getBranchCompiler() {
        return branchCompiler;
    }

    /**
     * Checkpoint compiler.
     *
     * @return the checkpoint compiler
     */
    public CheckpointCompiler getCheckpointCompiler() {
        return checkpointCompiler;
    }

    /**
     * Get the compiler for Ruby constant lookups.
     *
     * @return the constant compiler
     */
    public ConstantCompiler getConstantCompiler() {
        return constantCompiler;
    }

    /**
     * Instance variable compiler.
     *
     * @return the instance variable compiler
     */
    public InstanceVariableCompiler getInstanceVariableCompiler() {
        return instanceVariableCompiler;
    }

    /**
     * Global variable compiler.
     *
     * @return the global variable compiler
     */
    public GlobalVariableCompiler getGlobalVariableCompiler() {
        return globalVariableCompiler;
    }

    /**
     * Block yielding compiler.
     *
     * @return the yield compiler
     */
    public YieldCompiler getYieldCompiler() {
        return yieldCompiler;
    }

    /**
     * Block construction compiler.
     *
     * @return the block compiler
     */
    public BlockCompiler getBlockCompiler() {
        return blockCompiler;
    }

    /**
     * The compiler for argument processing or preparation.
     *
     * @return the arguments compiler
     */
    public ArgumentsCompiler getArgumentsCompiler() {
        return argumentsCompiler;
    }

    public static void buildArrayFromLocals(SkinnyMethodAdapter adapter2, int base, int arity) {
        if (arity == 0) {
            adapter2.getstatic(p(IRubyObject.class), "NULL_ARRAY", ci(IRubyObject[].class));
            return;
        }

        adapter2.pushInt(arity);
        adapter2.invokestatic(p(Helpers.class), "anewarrayIRubyObjects", sig(IRubyObject[].class, int.class));

        for (int i = 0; i < arity; ) {
            int j = 0;
            while (i + j < arity && j < Helpers.MAX_SPECIFIC_ARITY_OBJECT_ARRAY) {
                adapter2.aload(base + i + j);
                j++;
            }
            adapter2.pushInt(i);
            adapter2.invokestatic(p(Helpers.class), "aastoreIRubyObjects", sig(IRubyObject[].class, params(IRubyObject[].class, IRubyObject.class, j, int.class)));
            i += j;
        }
    }

    public String getUniqueSiteName(String name) {
        return "invokeOther" + getClassData().cacheFieldCount.getAndIncrement() + ":" + JavaNameMangler.mangleMethodName(name);
    }

    public ClassData getClassData() {
        return classData;
    }

    public void startMethod() {
        adapter.start();
    }

    public void endMethod() {
        adapter.end(new Runnable() {
            public void run() {
                for (IntHashMap.Entry<Type> entry : variableTypes.entrySet()) {
                    final int i = entry.getKey();
                    String name = variableNames.get(i);
                    adapter.local(i, name, entry.getValue());
                }
            }
        });
    }

    public void loadLocal(int i) {
        adapter.aload(i);
    }

    public void loadContext() {
        adapter.aload(signature.argOffset("context"));
    }

    public void loadSelfBlock() {
        int selfBlockOffset = signature.argOffset(JVMVisitor.SELF_BLOCK_NAME);
        if (selfBlockOffset == -1) {
            adapter.aconst_null();
        } else {
            adapter.aload(selfBlockOffset);
        }
    }

    public void loadStaticScope() {
        adapter.aload(signature.argOffset("scope"));
    }

    public void loadSelf() {
        adapter.aload(signature.argOffset("self"));
    }

    public void loadArgs() {
        adapter.aload(signature.argOffset("args"));
    }

    public void loadBlock() {
        adapter.aload(signature.argOffset(JVMVisitor.BLOCK_ARG_NAME));
    }

    public void loadFrameClass() {
        int superNameOffset = signature.argOffset(JVMVisitor.SUPER_NAME_NAME);

        if (superNameOffset == -1) {
            // load from self block
            loadSelfBlock();
            adapter.invokevirtual(p(Block.class), "getBinding", sig(Binding.class));
            adapter.invokevirtual(p(Binding.class), "getFrame", sig(Frame.class));
            adapter.invokevirtual(p(Frame.class), "getKlazz", sig(RubyModule.class));
        } else {
            // when present, should be second-to-last element in signature
            adapter.aload(signature.argCount() - 2);
        }
    }

    public void loadFrameName() {
        int superNameOffset = signature.argOffset(JVMVisitor.SUPER_NAME_NAME);

        if (superNameOffset == -1) {
            // load from self block
            loadSelfBlock();
            adapter.invokevirtual(p(Block.class), "getBinding", sig(Binding.class));
            adapter.invokevirtual(p(Binding.class), "getMethod", sig(String.class));
        } else {
            adapter.aload(superNameOffset);
        }
    }

    public void storeSelf() {
        adapter.astore(signature.argOffset("self"));
    }

    public void storeArgs() {
        adapter.astore(signature.argOffset("args"));
    }

    public void storeLocal(int i) {
        adapter.astore(i);
    }

    public void invokeVirtual(Type type, Method method) {
        adapter.invokevirtual(type.getInternalName(), method.getName(), method.getDescriptor());
    }

    public void invokeStatic(Type type, Method method) {
        adapter.invokestatic(type.getInternalName(), method.getName(), method.getDescriptor());
    }

    public void invokeHelper(String name, String sig) {
        adapter.invokestatic(p(Helpers.class), name, sig);
    }

    public void invokeHelper(String name, Class... x) {
        adapter.invokestatic(p(Helpers.class), name, sig(x));
    }

    public void invokeIRHelper(String name, String sig) {
        adapter.invokestatic(p(IRRuntimeHelpers.class), name, sig);
    }

    public void goTo(org.objectweb.asm.Label label) {
        adapter.go_to(label);
    }

    public void pushHandle(Handle handle) {
        adapter.getMethodVisitor().visitLdcInsn(handle);
    }

    public void mark(org.objectweb.asm.Label label) {
        adapter.label(label);
    }

    public void returnValue() {
        adapter.areturn();
    }

    public int newLocal(String name, Type type) {
        int index = variableCount++;
        if (type == Type.DOUBLE_TYPE || type == Type.LONG_TYPE) {
            variableCount++;
        }
        variableTypes.put(index, type);
        variableNames.put(index, name);
        return index;
    }

    public org.objectweb.asm.Label newLabel() {
        return new org.objectweb.asm.Label();
    }

    public void getStaticScope(String field) {
        adapter.getstatic(classData.clsName, field, ci(StaticScope.class));
        adapter.dup();
        Label after = newLabel();
        adapter.ifnonnull(after);
        adapter.pop();
        adapter.ldc(classData.visitor.staticScopeDescriptorMap.get(field));
        loadStaticScope();
        invokeHelper("restoreScope", StaticScope.class, String.class, StaticScope.class);
        adapter.dup();
        adapter.putstatic(classData.clsName, field, ci(StaticScope.class));
        adapter.label(after);
    }

    public void outline(String name, MethodType type, Runnable body) {
        SkinnyMethodAdapter oldAdapter = adapter;
        adapter = new SkinnyMethodAdapter(
                oldAdapter.getClassVisitor(),
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                name,
                sig(type),
                null,
                null);

        body.run();

        adapter.end();

        adapter = oldAdapter;
    }

    public void updateLineNumber(int lineNumber) {
        lastLine = lineNumber + 1;
        adapter.line(lastLine);
    }

    public int getLastLine() {
        return lastLine;
    }

    public enum BlockPassType {
        NONE(false, false),
        GIVEN(true, false),
        LITERAL(true, true);

        private final boolean given;
        private final boolean literal;

        BlockPassType(boolean given, boolean literal) {
            this.given = given;
            this.literal = literal;
        }

        public boolean given() {
            return given;
        }
        public boolean literal() {
            return literal;
        }
        public static BlockPassType fromIR(ClosureAcceptingInstr callInstr) {
            Operand closure = callInstr.getClosureArg();
            return closure != null ? ( callInstr.hasLiteralClosure() ? BlockPassType.LITERAL : BlockPassType.GIVEN) : BlockPassType.NONE;
        }
    }

    public SkinnyMethodAdapter adapter;
    private int variableCount = 0;
    private final IntHashMap<Type> variableTypes = new IntHashMap<>();
    private final IntHashMap<String> variableNames = new IntHashMap<>();
    protected final Signature signature;
    protected final ClassData classData;
    protected final ValueCompiler valueCompiler;
    protected final DynamicValueCompiler dynamicValueCompiler;
    protected final InvocationCompiler invocationCompiler;
    protected final BranchCompiler branchCompiler;
    protected final CheckpointCompiler checkpointCompiler;
    protected final ConstantCompiler constantCompiler;
    protected final InstanceVariableCompiler instanceVariableCompiler;
    protected final GlobalVariableCompiler globalVariableCompiler;
    protected final YieldCompiler yieldCompiler;
    protected final BlockCompiler blockCompiler;
    protected final ArgumentsCompiler argumentsCompiler;
    public int ipc = 0;  // counter for dumping instr index when in DEBUG
    private int lastLine = -1;
}
