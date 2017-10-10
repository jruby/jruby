/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006-2007 Thomas E Enebo <enebo@acm.org>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.parser;

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;

import org.jcodings.specific.USASCIIEncoding;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.ast.AssignableNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.DVarNode;
import org.jruby.ast.IScopedNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.LocalVarNode;
import org.jruby.ast.Node;
import org.jruby.ast.VCallNode;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScopeType;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.scope.DynamicScopeGenerator;
import org.jruby.runtime.scope.ManyVarsDynamicScope;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

/**
 * StaticScope represents lexical scoping of variables and module/class constants.
 * 
 * At a very high level every scopes enclosing scope contains variables in the next outer
 * lexical layer.  The enclosing scopes variables may or may not be reachable depending
 * on the scoping rules for variables (governed by BlockStaticScope and LocalStaticScope).
 * 
 * StaticScope also keeps track of current module/class that is in scope.  previousCRefScope
 * will point to the previous scope of the enclosing module/class (cref).
 * 
 */
public class StaticScope implements Serializable {
    private static final int MAX_SPECIALIZED_SIZE = 50;
    private static final long serialVersionUID = 3423852552352498148L;
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();

    // Next immediate scope.  Variable and constant scoping rules make use of this variable
    // in different ways.
    final protected StaticScope enclosingScope;

    // Live reference to module
    private transient RubyModule cref = null;

    // Next CRef down the lexical structure
    private StaticScope previousCRefScope = null;

    // Our name holder (offsets are assigned as variables are added)
    private ByteList[] variableNames;

    private int variableNamesLength;

    // A list of booleans indicating which variables are named captures from regexp
    private boolean[] namedCaptures;

    // Arity of this scope if there is one
    private Signature signature;

    // File name where this static scope came from or null if a native or artificial scope
    private String file;

    private DynamicScope dummyScope;

    protected IRScopeType scopeType;

    private static final ByteList[] NO_NAMES = new ByteList[0];

    private Type type;
    private boolean isBlockOrEval;
    private boolean isArgumentScope; // Is this block and argument scope of a define_method.

    private long commandArgumentStack;

    private int firstKeywordIndex = -1;

    // Method/Closure that this static scope corresponds to.  This is used to tell whether this
    // scope refers to a method scope or to determined IRScope of the parent of a compiling eval.
    private IRScope irScope;

    private RubyModule overlayModule;

    private volatile MethodHandle constructor;

    public enum Type {
        LOCAL, BLOCK, EVAL;

        public static Type fromOrdinal(int value) {
            return value < 0 || value >= values().length ? null : values()[value];
        }
    }

    /**
     *
     */
    protected StaticScope(Type type, StaticScope enclosingScope, String file) {
        this(type, enclosingScope, NO_NAMES);

        this.file = file;
    }

    /**
     * Construct a new static scope.
     *
     * @param type           the type of scope
     * @param enclosingScope the lexically containing scope.
     */
    protected StaticScope(Type type, StaticScope enclosingScope) {
        this(type, enclosingScope, NO_NAMES);
    }

    /**
     * Construct a new static scope. The array of strings should all be the
     * interned versions, since several other optimizations depend on being
     * able to do object equality checks.
     *
     * @param type           the type of scope
     * @param enclosingScope the lexically containing scope.
     * @param names          The list of interned String variable names.
     */
    protected StaticScope(Type type, StaticScope enclosingScope, ByteList[] names, int firstKeywordIndex) {
        assert names != null : "names is not null";

        this.enclosingScope = enclosingScope;
        this.variableNames = names;
        this.variableNamesLength = names.length;
        this.type = type;
        this.irScope = null;
        this.isBlockOrEval = (type != Type.LOCAL);
        this.isArgumentScope = !isBlockOrEval;
        this.firstKeywordIndex = firstKeywordIndex;
    }

    protected StaticScope(Type type, StaticScope enclosingScope, ByteList[] names) {
        this(type, enclosingScope, names, -1);
    }

    public int getFirstKeywordIndex() {
        return firstKeywordIndex;
    }

    public DynamicScope construct(DynamicScope parent) {
        MethodHandle constructor = this.constructor;

        if (constructor == null) constructor = acquireConstructor();

        try {
            return (DynamicScope) constructor.invokeExact(this, parent);
        } catch (Throwable e) {
            Helpers.throwException(e);
            return null; // not reached
        }
    }

    private synchronized MethodHandle acquireConstructor() {
        // check again
        MethodHandle constructor = this.constructor;

        if (constructor != null) return constructor;

        int numberOfVariables = getNumberOfVariables();

        if (numberOfVariables > MAX_SPECIALIZED_SIZE) {
            constructor = ManyVarsDynamicScope.CONSTRUCTOR;
        } else {
            constructor = DynamicScopeGenerator.generate(numberOfVariables);
        }

        this.constructor = constructor;

        return constructor;
    }

    public IRScope getIRScope() {
        return irScope;
    }

    public IRScopeType getScopeType() {
        return scopeType;
    }

    public void setScopeType(IRScopeType scopeType) {
        this.scopeType = scopeType;
    }

    public void setIRScope(IRScope irScope) {
        this.irScope = irScope;
        this.scopeType = irScope.getScopeType();
    }

    /**
     * Add a new variable to this (current) scope unless it is already defined in the
     * current scope.
     *
     * @param name of new variable
     * @return index of variable
     */
    public int addVariableThisScope(ByteList name) {
        int slot = exists(name);

        if (slot >= 0) return slot;

        // Clear constructor since we are adding a name
        constructor = null;

        // This is perhaps innefficient timewise?  Optimal spacewise
        growVariableNames(name);

        // Returns slot of variable
        return variableNames.length - 1;
    }

    @Deprecated
    public int addVariableThisScope(String stringName) {
        return addVariableThisScope(new ByteList(stringName.getBytes(), USASCIIEncoding.INSTANCE));
    }

    /**
     * Add a new named capture variable to this (current) scope.
     *
     * @param name name of variable.
     * @return index of variable
     */
    public int addNamedCaptureVariable(ByteList name) {
        int index = addVariableThisScope(name);

        growNamedCaptures(index);

        return index;
    }

    @Deprecated
    public int addNamedCaptureVariable(String name) {
        int index = addVariableThisScope(name);

        growNamedCaptures(index);

        return index;
    }

    /**
     * Add a new variable to this (current) scope unless it is already defined in any
     * reachable scope.
     *
     * @param name of new variable
     * @return index+depth merged location of scope
     */
    public int addVariable(ByteList name) {
        int slot = isDefined(name);

        if (slot >= 0) return slot;

        // Clear constructor since we are adding a name
        constructor = null;

        // This is perhaps innefficient timewise?  Optimal spacewise
        growVariableNames(name);

        // Returns slot of variable
        return variableNames.length - 1;
    }

    @Deprecated
    public int addVariable(String name) {
        return addVariable(new ByteList(name.getBytes(), USASCIIEncoding.INSTANCE));
    }

    @Deprecated
    public String[] getVariables() {
        String[] newVars = new String[variableNames.length];
        for (int i = 0; i < variableNames.length; i++) {
            newVars[i] = StringSupport.byteListAsString(variableNames[i]);
        }
        return newVars;
    }

    public ByteList[] getByteVariables() {
        return variableNames;
    }

    public int getNumberOfVariables() {
        return variableNamesLength;
    }

    public void setVariables(String[] names) {
        assert names != null : "names is not null";

        // Clear constructor since we are changing names
        constructor = null;

        variableNames = new ByteList[names.length];
        variableNamesLength = names.length;
        System.arraycopy(names, 0, variableNames, 0, names.length);
    }

    /**
     * Gets a constant back from lexical search from the cref in this scope.
     * As it is for defined? we will not forced resolution of autoloads nor
     * call const_defined
     */
    public IRubyObject getConstantDefined(String internedName) {
        IRubyObject result = cref.fetchConstant(internedName);

        if (result != null) return result;

        return previousCRefScope == null ? null : previousCRefScope.getConstantDefinedNoObject(internedName);
    }

    public IRubyObject getConstantDefinedNoObject(String internedName) {
        if (previousCRefScope == null) return null;

        return getConstantDefined(internedName);
    }

    public IRubyObject getConstant(String internedName) {
        IRubyObject result = getConstantInner(internedName);

        // If we could not find the constant from cref..then try getting from inheritence hierarchy
        return result == null ? cref.getConstantNoConstMissing(internedName) : result;
    }

    public IRubyObject getConstantInner(String internedName) {
        IRubyObject result = cref.fetchConstant(internedName);

        if (result != null) {
            return result == RubyObject.UNDEF ? cref.resolveUndefConstant(internedName) : result;
        }

        return previousCRefScope == null ? null : previousCRefScope.getConstantInnerNoObject(internedName);
    }

    private IRubyObject getConstantInnerNoObject(String internedName) {
        if (previousCRefScope == null) return null;

        return getConstantInner(internedName);
    }

    /**
     * Next outer most scope in list of scopes.  An enclosing scope may have no direct scoping
     * relationship to its child.  If I am in a localScope and then I enter something which
     * creates another localScope the enclosing scope will be the first scope, but there are
     * no valid scoping relationships between the two.  Methods which walk the enclosing scopes
     * are responsible for enforcing appropriate scoping relationships.
     *
     * @return the parent scope
     */
    public StaticScope getEnclosingScope() {
        return enclosingScope;
    }

    /**
     * Does the variable exist?
     *
     * @param name of the variable to find
     * @return index of variable or -1 if it does not exist
     */
    public int exists(ByteList name) {
        return findVariableName(name);
    }

    @Deprecated
    public int exists(String name) {
        return findVariableName(new ByteList(name.getBytes(), USASCIIEncoding.INSTANCE));
    }

    private int findVariableName(ByteList name) {
        for (int i = 0; i < variableNames.length; i++) {
            if (name.equal(variableNames[i])) return i;
        }
        return -1;
    }

    /**
     * Is this name in the visible to the current scope
     *
     * @param name to be looked for
     * @return a location where the left-most 16 bits of number of scopes down it is and the
     * right-most 16 bits represents its index in that scope
     */
    public int isDefined(ByteList name) {
        return isDefined(name, 0);
    }

    @Deprecated
    public int isDefined(String name) {
        return isDefined(name, 0);
    }

    /**
     * Make a DASgn or LocalAsgn node based on scope logic
     *
     * @param position
     * @param name
     * @param value
     * @return
     */
    public AssignableNode assign(ISourcePosition position, ByteList name, Node value) {
        return assign(position, name, value, this, 0);
    }

    @Deprecated
    public AssignableNode assign(ISourcePosition position, String name, Node value) {
        return assign(position, name, value, this, 0);
    }

    /**
     * Register a keyword argument with this staticScope.  It additionally will track
     * where the first keyword argument started so we can test and tell whether we have
     * a kwarg or an ordinary variable during live execution (See keywordExists).
     * @param position
     * @param name
     * @param value
     * @return
     */
    public AssignableNode assignKeyword(ISourcePosition position, ByteList name, Node value) {
        AssignableNode assignment = assign(position, name, value, this, 0);

        // register first keyword index encountered
        if (firstKeywordIndex == -1) firstKeywordIndex = ((IScopedNode) assignment).getIndex();

        return assignment;
    }

    @Deprecated
    public AssignableNode assignKeyword(ISourcePosition position, String name, Node value) {
        return assignKeyword(position, StringSupport.stringAsByteList(name), value);
    }

    public boolean keywordExists(String name) {
        int slot = exists(name);

        return slot >= 0 && firstKeywordIndex != -1 && slot >= firstKeywordIndex;
    }

    /**
     * Get all visible variables that we can see from this scope that have been assigned
     * (e.g. seen so far)
     *
     * @return a list of all names (sans $~ and $_ which are special names)
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

    public int isDefined(ByteList name, int depth) {
        if (isBlockOrEval) {
            int slot = exists(name);
            if (slot >= 0) return (depth << 16) | slot;

            return enclosingScope.isDefined(name, depth + 1);
        } else {
            return (depth << 16) | exists(name);
        }
    }

    public int isDefined(String name, int depth) {
        return isDefined(new ByteList(name.getBytes(), USASCIIEncoding.INSTANCE), depth);
    }

    public AssignableNode addAssign(ISourcePosition position, ByteList name, Node value) {
        int slot = addVariable(name);
        // No bit math to store level since we know level is zero for this case
        return new DAsgnNode(position, name, slot, value);
    }

    @Deprecated
    public AssignableNode addAssign(ISourcePosition position, String name, Node value) {
        return addAssign(position, new ByteList(name.getBytes(), USASCIIEncoding.INSTANCE), value);
    }

    public AssignableNode assign(ISourcePosition position, ByteList name, Node value,
                                 StaticScope topScope, int depth) {
        int slot = exists(name);

        // We can assign if we already have variable of that name here or we are the only
        // scope in the chain (which Local scopes always are).
        if (slot >= 0) {
            return isBlockOrEval ? new DAsgnNode(position, name, ((depth << 16) | slot), value)
                    : new LocalAsgnNode(position, name, ((depth << 16) | slot), value);
        } else if (!isBlockOrEval && (topScope == this)) {
            slot = addVariable(name);

            return new LocalAsgnNode(position, name, slot, value);
        }

        // If we are not a block-scope and we go there, we know that 'topScope' is a block scope
        // because a local scope cannot be within a local scope
        // If topScope was itself it would have created a LocalAsgnNode above.
        return isBlockOrEval ? enclosingScope.assign(position, name, value, topScope, depth + 1)
                : topScope.addAssign(position, name, value);
    }

    @Deprecated
    public AssignableNode assign(ISourcePosition position, String name, Node value,
                                 StaticScope topScope, int depth) {
        return assign(position, new ByteList(name.getBytes(), USASCIIEncoding.INSTANCE), value, topScope, depth);
    }

    public Node declare(ISourcePosition position, ByteList name, int depth) {
        int slot = exists(name);

        if (slot >= 0) {
            return isBlockOrEval ? new DVarNode(position, ((depth << 16) | slot), name) : new LocalVarNode(position, ((depth << 16) | slot), name);
        }

        return isBlockOrEval ? enclosingScope.declare(position, name, depth + 1) : new VCallNode(position, name);
    }

    /**
     * Make a DVar or LocalVar node based on scoping logic
     *
     * @param position the location that in the source that the new node will come from
     * @param name     of the variable to be created is named
     * @return a DVarNode or LocalVarNode
     */
    public Node declare(ISourcePosition position, ByteList name) {
        return declare(position, name, 0);
    }

    @Deprecated
    public Node declare(ISourcePosition position, String name) {
        return declare(position, new ByteList(name.getBytes(), USASCIIEncoding.INSTANCE));
    }
    /**
     * Gets the Local Scope relative to the current Scope.  For LocalScopes this will be itself.
     * Blocks will contain the LocalScope it contains.
     *
     * @return localScope
     */

    public StaticScope getLocalScope() {
        return (type != Type.BLOCK) ? this : enclosingScope.getLocalScope();
    }

    /**
     * Get the live CRef module associated with this scope.
     *
     * @return the live module
     */
    public RubyModule getModule() {
        return cref;
    }

    public StaticScope getPreviousCRefScope() {
        return previousCRefScope;
    }

    public void setPreviousCRefScope(StaticScope crefScope) {
        this.previousCRefScope = crefScope;
    }

    public void setModule(RubyModule module) {
        this.cref = module;

        for (StaticScope scope = getEnclosingScope(); scope != null; scope = scope.getEnclosingScope()) {
            if (scope.cref != null) {
                previousCRefScope = scope;
                return;
            }
        }
    }

    /**
     * Update current scoping structure to populate with proper cref scoping values.  This should
     * be called at any point when you reference a scope for the first time.  For the interpreter
     * this is done in a small number of places (defnNode, defsNode, and getBlock).  The compiler
     * does this in the same places.
     *
     * @return the current cref, though this is largely an implementation detail
     */
    public RubyModule determineModule() {
        if (cref == null) {
            cref = getEnclosingScope().determineModule();

            assert cref != null : "CRef is always created before determine happens";

            previousCRefScope = getEnclosingScope().previousCRefScope;
        }

        return cref;
    }

    public boolean isBlockScope() {
        return isBlockOrEval;
    }

    /**
     * Argument scopes represent scopes which contain arguments for zsuper.  All LocalStaticScopes
     * are argument scopes and BlockStaticScopes can be when they are used by define_method.
     */
    public boolean isArgumentScope() {
        return isArgumentScope;
    }

    public void makeArgumentScope() {
        this.isArgumentScope = true;
    }

    /**
     * For all block or method associated with static scopes this will return the signature for that
     * signature-providing scope.  module bodies and other non-arity specific code will return null.
     */
    public Signature getSignature() {
        return signature;
    }

    /**
     * This happens in when first defining ArgsNodes or when reifying a method from AOT.
     */
    public void setSignature(Signature signature) {
        this.signature = signature;
    }

    public DynamicScope getDummyScope() {
        return dummyScope == null ? dummyScope = DynamicScope.newDynamicScope(this) : dummyScope;
    }

    public void setCommandArgumentStack(long commandArgumentStack) {
        this.commandArgumentStack = commandArgumentStack;
    }

    public long getCommandArgumentStack() {
        return commandArgumentStack;
    }

    private void growVariableNames(ByteList name) {
        ByteList[] newVariableNames = new ByteList[variableNames.length + 1];
        System.arraycopy(variableNames, 0, newVariableNames, 0, variableNames.length);
        variableNames = newVariableNames;
        variableNamesLength = newVariableNames.length;
        variableNames[variableNames.length - 1] = name;
    }

    private void growNamedCaptures(int index) {
        boolean[] namedCaptures = this.namedCaptures;
        boolean[] newNamedCaptures;
        if (namedCaptures != null) {
            newNamedCaptures = new boolean[Math.max(index + 1, namedCaptures.length)];
            System.arraycopy(namedCaptures, 0, newNamedCaptures, 0, namedCaptures.length);
        } else {
            newNamedCaptures = new boolean[index + 1];
        }
        newNamedCaptures[index] = true;
        this.namedCaptures = newNamedCaptures;
    }

    public boolean isNamedCapture(int index) {
        boolean[] namedCaptures = this.namedCaptures;
        return namedCaptures != null && index < namedCaptures.length && namedCaptures[index];
    }

    @Override
    public String toString() {
        // FIXME: Do we need to persist cref as well?
        return "StaticScope(" + type + "):" + Arrays.toString(variableNames);
    }

    public Type getType() {
        return type;
    }

    public String getFile() {
        return file;
    }

    public StaticScope duplicate() {
        StaticScope dupe = new StaticScope(type, enclosingScope, variableNames == null ? NO_NAMES : variableNames);
        // irScope is not guaranteed to be set onto StaticScope until it is executed for the first time.
        // We can call duplicate before its first execution.
        if (irScope != null) dupe.setIRScope(irScope);
        dupe.setScopeType(scopeType);
        dupe.setPreviousCRefScope(previousCRefScope);
        dupe.setModule(cref);
        dupe.setSignature(signature);

        return dupe;
    }

    public RubyModule getOverlayModuleForRead() {
        return overlayModule;
    }

    public RubyModule getOverlayModuleForWrite(ThreadContext context) {
        RubyModule omod = overlayModule;
        if (omod == null) {
            overlayModule = omod = RubyModule.newModule(context.runtime);
        }
        return omod;
    }
}
