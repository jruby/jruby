package org.jruby.ir;

import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jruby.ast.MethodDefNode;
import org.jruby.internal.runtime.methods.IRMethodArgs;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.parser.StaticScope;

public class IRMethod extends IRScope {
    public final boolean isInstanceMethod;

    // Argument description of the form [:req, "a"], [:opt, "b"] ..
    private List<String[]> argDesc;

    // Signatures to the jitted versions of this method
    private Map<Integer, MethodType> signatures;

    // Method name in the jitted version of this method
    private String jittedName;

    private MethodDefNode defn;

    public IRMethod(IRManager manager, IRScope lexicalParent, MethodDefNode defn, String name,
            boolean isInstanceMethod, int lineNumber, StaticScope staticScope) {
        super(manager, lexicalParent, name, lexicalParent.getFileName(), lineNumber, staticScope);

        this.defn = defn;
        this.isInstanceMethod = isInstanceMethod;
        this.argDesc = new ArrayList<>();
        this.signatures = new HashMap<>();

        if (!getManager().isDryRun() && staticScope != null) {
            staticScope.setIRScope(this);
            staticScope.setScopeType(this.getScopeType());
        }
    }

    public synchronized InterpreterContext lazilyAcquireInterpreterContext() {
        if (defn != null) {
            IRBuilder.topIRBuilder(getManager(), this).defineMethodInner(defn, getLexicalParent());

            defn = null;
        }

        return interpreterContext;
    }

    public synchronized List<BasicBlock> prepareForCompilation() {
        if (defn != null) lazilyAcquireInterpreterContext();

        return super.prepareForCompilation();
    }

    @Override
    public IRScopeType getScopeType() {
        return isInstanceMethod ? IRScopeType.INSTANCE_METHOD : IRScopeType.CLASS_METHOD;
    }

    public void addArgDesc(IRMethodArgs.ArgType type, String argName) {
        argDesc.add(new String[]{type.name(), argName});
    }

    public List<String[]> getArgDesc() {
        return argDesc;
    }

    @Override
    protected LocalVariable findExistingLocalVariable(String name, int scopeDepth) {
        assert scopeDepth == 0: "Local variable depth in IRMethod should always be zero (" + name + " had depth of " + scopeDepth + ")";
        return localVars.get(name);
    }

    @Override
    public LocalVariable getLocalVariable(String name, int scopeDepth) {
        LocalVariable lvar = findExistingLocalVariable(name, scopeDepth);
        if (lvar == null) lvar = getNewLocalVariable(name, scopeDepth);
        return lvar;
    }

    public void addNativeSignature(int arity, MethodType signature) {
        signatures.put(arity, signature);
    }

    public MethodType getNativeSignature(int arity) {
        return signatures.get(arity);
    }

    public Map<Integer, MethodType> getNativeSignatures() {
        return Collections.unmodifiableMap(signatures);
    }

    public String getJittedName() {
        return jittedName;
    }

    public void setJittedName(String jittedName) {
        this.jittedName = jittedName;
    }
}
