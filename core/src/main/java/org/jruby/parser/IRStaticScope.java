package org.jruby.parser;

import org.jruby.ast.AssignableNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.DVarNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.LocalVarNode;
import org.jruby.ast.Node;
import org.jruby.ast.VCallNode;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScopeType;
import org.jruby.lexer.yacc.ISourcePosition;

// SSS FIXME: At some point, we should merge StaticScope and IRStaticScope.
// This should be done in conjuction with RubyClass stack removal.
public class IRStaticScope extends StaticScope {
    private static final long serialVersionUID = 3423852552352498148L;

    private static final String[] NO_NAMES = new String[0];

    private Type type;
    private boolean isBlockOrEval;
    private boolean isArgumentScope; // Is this block and argument scope of a define_method (for the purposes of zsuper).

    private int scopeId;
    private IRScopeType scopeType;
    private IRScope irScope; // Method/Closure that this static scope corresponds to

    protected IRStaticScope(Type type, StaticScope enclosingScope) {
        this(type, enclosingScope, NO_NAMES);
    }

    protected IRStaticScope(Type type, StaticScope enclosingScope, String[] names) {
        super(enclosingScope, names);
        this.type = type;
        this.irScope = null;
        this.isBlockOrEval = (type != Type.LOCAL);
        this.isArgumentScope = !isBlockOrEval;
    }

    public StaticScope getLocalScope() {
        return (type != Type.BLOCK) ? this : enclosingScope.getLocalScope();
    }

    public int isDefined(String name, int depth) {
        if (isBlockOrEval) {
            int slot = exists(name); 
            if (slot >= 0) return (depth << 16) | slot;
            
            return enclosingScope.isDefined(name, depth + 1);
        }
        else {
            return (depth << 16) | exists(name);
        }
    }

    @Override
    public boolean isBlockScope() {
        return isBlockOrEval;
    }

    @Override
    public boolean isArgumentScope() {
        return isArgumentScope;
    }

    @Override
    public void makeArgumentScope() {
        this.isArgumentScope = true;
    }

    /**
     * @see org.jruby.parser.StaticScope#getAllNamesInScope()
     */
    public String[] getAllNamesInScope() {
        String[] names = getVariables();        
        if (isBlockOrEval) {
            String[] ourVariables = names;
            String[] variables = enclosingScope.getAllNamesInScope();
            
            // we know variables cannot be null since this IRStaticScope always returns a non-null array
            names = new String[variables.length + ourVariables.length];
            
            System.arraycopy(variables, 0, names, 0, variables.length);
            System.arraycopy(ourVariables, 0, names, variables.length, ourVariables.length);
        }
        
        return names;
    }

    public AssignableNode addAssign(ISourcePosition position, String name, Node value) {
        int slot = addVariable(name);
        // No bit math to store level since we know level is zero for this case
        return new DAsgnNode(position, name, slot, value);
    }
    
    public AssignableNode assign(ISourcePosition position, String name, Node value, 
            StaticScope topScope, int depth) {
        int slot = exists(name);
        
        // We can assign if we already have variable of that name here or we are the only
        // scope in the chain (which Local scopes always are).
        if (slot >= 0) {
            return isBlockOrEval ? new DAsgnNode(position, name, ((depth << 16) | slot), value) 
                           : new LocalAsgnNode(position, name, ((depth << 16) | slot), value);
        } else if (!isBlockOrEval && (topScope == this)) {
            slot = addVariable(name);

            return new LocalAsgnNode(position, name, slot , value);
        }
        
        // If we are not a block-scope and we go there, we know that 'topScope' is a block scope 
        // because a local scope cannot be within a local scope
        // If topScope was itself it would have created a LocalAsgnNode above.
        return isBlockOrEval ? enclosingScope.assign(position, name, value, topScope, depth + 1)
                       : ((IRStaticScope)topScope).addAssign(position, name, value);
    }

    public Node declare(ISourcePosition position, String name, int depth) {
        int slot = exists(name);
        
        if (slot >= 0) {
            return isBlockOrEval ? new DVarNode(position, ((depth << 16) | slot), name) : new LocalVarNode(position, ((depth << 16) | slot), name);
        }

        return isBlockOrEval ? enclosingScope.declare(position, name, depth + 1) : new VCallNode(position, name);
    }
    
    @Override
    public String toString() {
        return "IRStaticScope(" + type + "):" + super.toString();
    }

    public IRScope getIRScope() {
        return irScope;
    }

    public int getScopeId() {
        return scopeId;
    }

    public IRScopeType getScopeType() {
        return scopeType;
    }

    public void setIRScope(IRScope irScope, boolean isForLoopBody) {
        if (!isForLoopBody) {
            this.irScope = irScope;
        }
        this.scopeId = irScope.getScopeId();
        this.scopeType = irScope.getScopeType();
    }

    public void setIRScope(IRScope irScope) {
        setIRScope(irScope, false);
    }

    @Override
    public int getNumberOfVariables() {
        return irScope == null ? super.getNumberOfVariables() : irScope.getUsedVariablesCount();
    }

    @Override
    public Type getType() {
        return type;
    }
}
