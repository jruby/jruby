package org.jruby.ir;

import org.jruby.RubySymbol;
import org.jruby.ast.DefNode;
import org.jruby.ast.InstAsgnNode;
import org.jruby.ast.InstVarNode;
import org.jruby.ast.Node;
import org.jruby.ast.visitor.AbstractNodeVisitor;
import org.jruby.ir.instructions.GetFieldInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.PutFieldInstr;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.runtime.ivars.MethodData;
import org.jruby.util.ByteList;

import java.util.ArrayList;
import java.util.List;

public class IRMethod extends IRScope {
    public final boolean isInstanceMethod;

    // Argument description
    protected ArgumentDescriptor[] argDesc = ArgumentDescriptor.EMPTY_ARRAY;

    private volatile DefNode defNode;

    public IRMethod(IRManager manager, IRScope lexicalParent, DefNode defn, ByteList name,
            boolean isInstanceMethod, int lineNumber, StaticScope staticScope, int coverageMode) {
        super(manager, lexicalParent, name, lineNumber, staticScope, coverageMode);

        this.defNode = defn;
        this.isInstanceMethod = isInstanceMethod;


        if (staticScope != null) {
            staticScope.setIRScope(this);
        }
    }

    @Override
    public boolean hasBeenBuilt() {
        return defNode == null;
    }

    public MethodData getMethodData() {
        List<String> ivarNames = new ArrayList<>();

        DefNode def = defNode;
        if (def != null) {
            // walk AST
            def.getBodyNode().accept(new AbstractNodeVisitor<Object>() {
                @Override
                protected Object defaultVisit(Node node) {
                    if (node == null) return null;

                    if (node instanceof InstVarNode) {
                        ivarNames.add(((InstVarNode) node).getName().idString());
                    } else if (node instanceof InstAsgnNode) {
                        ivarNames.add(((InstAsgnNode) node).getName().idString());
                    }

                    node.childNodes().forEach(this::defaultVisit);

                    return null;
                }
            });
        } else {
            InterpreterContext context = lazilyAcquireInterpreterContext();

            // walk instructions
            for (Instr i : context.getInstructions()) {
                switch (i.getOperation()) {
                    case GET_FIELD:
                        ivarNames.add(((GetFieldInstr) i).getId());
                        break;
                    case PUT_FIELD:
                        ivarNames.add(((PutFieldInstr) i).getId());
                        break;
                }
            }
        }

        return new MethodData(getId(), getFile(), ivarNames);
    }

    @Override
    public InterpreterContext builtInterpreterContext() {
        return lazilyAcquireInterpreterContext();
    }

    final InterpreterContext lazilyAcquireInterpreterContext() {
        if (!hasBeenBuilt()) buildMethodImpl();

        return interpreterContext;
    }

    private synchronized void buildMethodImpl() {
        if (hasBeenBuilt()) return;

        IRBuilder.topIRBuilder(getManager(), this).
                defineMethodInner(defNode, getLexicalParent(), getCoverageMode()); // sets interpreterContext
        this.defNode = null;
    }

    public BasicBlock[] prepareForCompilation() {
        buildMethodImpl();

        return super.prepareForCompilation();
    }

    @Override
    public IRScopeType getScopeType() {
        return isInstanceMethod ? IRScopeType.INSTANCE_METHOD : IRScopeType.CLASS_METHOD;
    }

    @Override
    protected LocalVariable findExistingLocalVariable(RubySymbol name, int scopeDepth) {
        assert scopeDepth == 0: "Local variable depth in IRMethod should always be zero (" + name + " had depth of " + scopeDepth + ")";
        return localVars.get(name);
    }

    @Override
    public LocalVariable getLocalVariable(RubySymbol name, int scopeDepth) {
        LocalVariable lvar = findExistingLocalVariable(name, scopeDepth);
        if (lvar == null) lvar = getNewLocalVariable(name, scopeDepth);
        return lvar;
    }

    public ArgumentDescriptor[] getArgumentDescriptors() {
        return argDesc;
    }


    /**
     * Set upon completion of IRBuild of this IRMethod.
     */
    public void setArgumentDescriptors(ArgumentDescriptor[] argDesc) {
        this.argDesc = argDesc;
    }
}
