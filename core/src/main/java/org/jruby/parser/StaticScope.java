/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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
import java.util.ArrayList;
import java.util.Arrays;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBasicObject;
import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.api.Convert;
import org.jruby.ast.AssignableNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.DVarNode;
import org.jruby.ast.IScopedNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.LocalVarNode;
import org.jruby.ast.Node;
import org.jruby.ast.VCallNode;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScopeType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.scope.DynamicScopeGenerator;
import org.jruby.runtime.scope.ManyVarsDynamicScope;

import static org.jruby.api.Convert.asSymbol;
import static org.jruby.api.Define.defineModule;

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
public class StaticScope implements Serializable, Cloneable {
    public static final int MAX_SPECIALIZED_SIZE = 50;
    private static final long serialVersionUID = 3423852552352498148L;

    // Next immediate scope.  Variable and constant scoping rules make use of this variable
    // in different ways.
    protected StaticScope enclosingScope;

    // Live reference to module
    private transient RubyModule cref = null;

    // Next CRef down the lexical structure
    private StaticScope previousCRefScope = null;

    // Our name holder (offsets are assigned as variables are added) [these are symbol strings.  Use
    // as key to Symbol table for actual encoded versions].
    private String[] variableNames;

    private int variableNamesLength;

    // Arity of this scope if there is one
    private Signature signature;

    // File name where this static scope came from or null if a native or artificial scope
    private String file;

    private DynamicScope dummyScope;

    protected IRScopeType scopeType;

    private static final String[] NO_NAMES = new String[0];

    private Type type;
    private boolean isBlockOrEval;
    private boolean isArgumentScope; // Is this block and argument scope of a define_method.

    private int firstKeywordIndex;

    // Method/Closure that this static scope corresponds to.  This is used to tell whether this
    // scope refers to a method scope or to determined IRScope of the parent of a compiling eval.
    private IRScope irScope;

    private RubyModule overlayModule;

    private volatile MethodHandle constructor;

    private volatile Collection<String> ivarNames;

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
        this(type, enclosingScope, file, NO_NAMES, -1);
    }

    /**
     * Construct a new static scope.
     *
     * @param type           the type of scope
     * @param enclosingScope the lexically containing scope.
     */
    protected StaticScope(Type type, StaticScope enclosingScope) {
        this(type, enclosingScope, null, NO_NAMES, -1);
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
    protected StaticScope(Type type, StaticScope enclosingScope, String[] names, int firstKeywordIndex) {
        this(type, enclosingScope, null ,names, firstKeywordIndex);
    }

    protected StaticScope(Type type, StaticScope enclosingScope, String file, String[] names, int firstKeywordIndex) {
        assert names != null : "names is not null";

        this.enclosingScope = enclosingScope;
        this.variableNames = names;
        this.variableNamesLength = names.length;
        this.type = type;
        if (enclosingScope != null) this.scopeType = enclosingScope.getScopeType();
        this.isBlockOrEval = (type != Type.LOCAL);
        this.isArgumentScope = !isBlockOrEval;
        this.firstKeywordIndex = firstKeywordIndex;
        this.file = file;
    }

    protected StaticScope(Type type, StaticScope enclosingScope, String[] names) {
        this(type, enclosingScope, null, names, -1);
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
    public int addVariableThisScope(String name) {
        int slot = exists(name);

        if (slot >= 0) return slot;

        // Clear constructor since we are adding a name
        constructor = null;

        // This is perhaps innefficient timewise?  Optimal spacewise
        growVariableNames(name);

        // Returns slot of variable
        return variableNames.length - 1;
    }

    /**
     * Add a new variable to this (current) scope unless it is already defined in any
     * reachable scope.
     *
     * @param name of new variable
     * @return index+depth merged location of scope
     */
    public int addVariable(String name) {
        int slot = isDefined(name);

        if (slot >= 0) return slot;

        // Clear constructor since we are adding a name
        constructor = null;

        // This is perhaps innefficient timewise?  Optimal spacewise
        growVariableNames(name);

        // Returns slot of variable
        return variableNames.length - 1;
    }

    public String[] getVariables() {
        return variableNames;
    }

    public int getNumberOfVariables() {
        return variableNamesLength;
    }

    public void setVariables(String[] names) {
        assert names != null : "names is not null";

        // Clear constructor since we are changing names
        constructor = null;

        variableNames = new String[names.length];
        variableNamesLength = names.length;
        System.arraycopy(names, 0, variableNames, 0, names.length);
    }

    @Deprecated(since = "10.0")
    public IRubyObject getConstantDefined(String internedName) {
        return getConstantDefined(cref.getRuntime().getCurrentContext(), internedName);
    }

    /**
     * Gets a constant back from lexical search from the cref in this scope.
     * As it is for defined? we will not forced resolution of autoloads nor
     * call const_defined
     */
    public IRubyObject getConstantDefined(ThreadContext context, String internedName) {
        IRubyObject result = cref.fetchConstant(context, internedName);

        if (result != null) return result;

        return previousCRefScope == null ? null : previousCRefScope.getConstantDefinedNoObject(context, internedName);
    }

    @Deprecated(since = "10.0")
    public IRubyObject getConstantDefinedNoObject(String internedName) {
        return getConstantDefinedNoObject(cref.getRuntime().getCurrentContext(), internedName);
    }

    private IRubyObject getConstantDefinedNoObject(ThreadContext context, String internedName) {
        return previousCRefScope == null ? null : getConstantDefined(context, internedName);
    }

    @Deprecated(since = "10.0")
    public IRubyObject getConstant(String internedName) {
        return getConstant(cref.getRuntime().getCurrentContext(), internedName);
    }

    public IRubyObject getConstant(ThreadContext context, String internedName) {
        IRubyObject result = getScopedConstant(context, internedName);

        // If we could not find the constant from cref..then try getting from inheritance hierarchy
        return result == null ? cref.getConstantNoConstMissing(context, internedName) : result;
    }

    @Deprecated(since = "10.0")
    public IRubyObject getConstantInner(String internedName) {
        return getScopedConstant(cref.getRuntime().getCurrentContext(), internedName);
    }

    public IRubyObject getScopedConstant(ThreadContext context, String internedName) {
        IRubyObject result = cref.getConstantWithAutoload(context, internedName, RubyBasicObject.UNDEF, true);

        // If we had a failed autoload, give up hierarchy search
        if (result == RubyBasicObject.UNDEF) return null;
        if (result != null) return result;

        return previousCRefScope == null ? null : previousCRefScope.getConstantInnerNoObject(context, internedName);
    }

    private IRubyObject getConstantInnerNoObject(ThreadContext context, String internedName) {
        return previousCRefScope == null ? null : getScopedConstant(context, internedName);
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

    public void setEnclosingScope(StaticScope parent) {
        this.enclosingScope = parent;
    }

    /**
     * Does the variable exist?
     *
     * @param name of the variable to find
     * @return index of variable or -1 if it does not exist
     */
    public int exists(String name) {
        return findVariableName(name);
    }

    private int findVariableName(String name) {
        for (int i = 0; i < variableNames.length; i++) {
            if (name.equals(variableNames[i])) return i;
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
    public int isDefined(String name) {
        return isDefined(name, 0);
    }

    /**
     * Make a DASgn or LocalAsgn node based on scope logic
     *
     * Note: This is private code made public only for parser.
     */
    public AssignableNode assign(int line, RubySymbol name, Node value) {
        return assign(line, name, value, this, 0);
    }

    /**
     * Register a keyword argument with this staticScope.  It additionally will track
     * where the first keyword argument started so we can test and tell whether we have
     * a kwarg or an ordinary variable during live execution (See keywordExists).
     */
    public AssignableNode assignKeyword(int line, RubySymbol symbolID, Node value) {
        AssignableNode assignment = assign(line, symbolID, value, this, 0);

        // register first keyword index encountered
        if (firstKeywordIndex == -1) firstKeywordIndex = ((IScopedNode) assignment).getIndex();

        return assignment;
    }

    public boolean keywordExists(String name) {
        if (name.equals("_")) return true;
        int slot = exists(name);

        return slot >= 0 && firstKeywordIndex != -1 &&
                slot >= firstKeywordIndex  && slot < firstKeywordIndex + signature.kwargs();
    }

    /**
     * Get all visible variables that we can see from this scope that have been assigned
     * (e.g. seen so far)
     *
     * @return a list of all names (sans $~ and $_ which are special names)
     */
    public String[] getAllNamesInScope() {
        return collectVariables(ArrayList::new, ArrayList::add).stream().toArray(String[]::new);
    }

    /**
     * Populate a deduplicated collection of variable names in scope using the given functions.
     *
     * This may include variables that are not strictly Ruby local variable names, so the consumer should validate
     * names as appropriate.
     *
     * @param collectionFactory used to construct the collection
     * @param collectionPopulator used to pass values into the collection
     * @param <T> resulting collection type
     * @return populated collection
     */
    public <T> T collectVariables(IntFunction<T> collectionFactory, BiConsumer<T, String> collectionPopulator) {
        StaticScope current = this;

        T collection = collectionFactory.apply(current.variableNamesLength);

        HashMap<String, Object> dedup = new HashMap<>();

        while (current.isBlockOrEval) {
            for (String name : current.variableNames) {
                dedup.computeIfAbsent(name, key -> {collectionPopulator.accept(collection, key); return key;});
            }
            current = current.enclosingScope;
        }

        // once more for method scope
        for (String name : current.variableNames) {
            dedup.computeIfAbsent(name, key -> {collectionPopulator.accept(collection, key); return key;});
        }

        return collection;
    }

    /**
     * @param runtime
     * @return ""
     * @deprecated Use {@link StaticScope#getLocalVariables(ThreadContext)} instead.
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public RubyArray getLocalVariables(Ruby runtime) {
        return getLocalVariables(runtime.getCurrentContext());
    }

    /**
     * Convenience wrapper around {@link #collectVariables(IntFunction, BiConsumer)}.
     *
     * @param context the current context
     * @return populated RubyArray
     */
    public RubyArray getLocalVariables(ThreadContext context) {
        return collectVariables(
                context.runtime::newArray,
                (array, id) -> {
                    RubySymbol symbol = Convert.asSymbol(context, id);
                    if (symbol.validLocalVariableName()) array.append(context, symbol);
                });
    }

    public int isDefined(String name, int depth) {
        if (isBlockOrEval) {
            int slot = exists(name);
            if (slot >= 0) return (depth << 16) | slot;

            return enclosingScope.isDefined(name, depth + 1);
        } else {
            return (depth << 16) | exists(name);
        }
    }

    public AssignableNode addAssign(int line, RubySymbol symbolID, Node value) {
        int slot = addVariable(symbolID.idString());
        // No bit math to store level since we know level is zero for this case
        return new DAsgnNode(line, symbolID, slot, value);
    }

    public AssignableNode assign(int line, RubySymbol symbolID, Node value, StaticScope topScope, int depth) {
        String id = symbolID.idString();
        int slot = exists(id);

        // We can assign if we already have variable of that name here or we are the only
        // scope in the chain (which Local scopes always are).
        if (slot >= 0) {
            return isBlockOrEval ? new DAsgnNode(line, symbolID, ((depth << 16) | slot), value)
                    : new LocalAsgnNode(line, symbolID, ((depth << 16) | slot), value);
        } else if (!isBlockOrEval && (topScope == this)) {
            slot = addVariable(id);

            return new LocalAsgnNode(line, symbolID, slot, value);
        }

        // If we are not a block-scope and we go there, we know that 'topScope' is a block scope
        // because a local scope cannot be within a local scope
        // If topScope was itself it would have created a LocalAsgnNode above.
        return isBlockOrEval ?
                enclosingScope.assign(line, symbolID, value, topScope, depth + 1) :
                topScope.addAssign(line, symbolID, value);
    }

    // Note: This is private code made public only for parser.
    public Node declare(int line, RubySymbol symbolID, int depth) {
        int slot = exists(symbolID.idString());

        if (slot >= 0) {
            return isBlockOrEval ?
                    new DVarNode(line, ((depth << 16) | slot), symbolID) :
                    new LocalVarNode(line, ((depth << 16) | slot), symbolID);
        }

        return isBlockOrEval ? enclosingScope.declare(line, symbolID, depth + 1) : new VCallNode(line, symbolID);
    }

    /**
     * Make a DVar or LocalVar node based on scoping logic
     *
     * @param line the location that in the source that the new node will come from
     * @param symbolID of the variable to be created is named
     * @return a DVarNode or LocalVarNode
     *
     * Note: This is private code made public only for parser.
     */
    public Node declare(int line, RubySymbol symbolID) {
        return declare(line, symbolID, 0);
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

    private void growVariableNames(String name) {
        String[] newVariableNames = new String[variableNames.length + 1];
        System.arraycopy(variableNames, 0, newVariableNames, 0, variableNames.length);
        variableNames = newVariableNames;
        variableNamesLength = newVariableNames.length;
        variableNames[variableNames.length - 1] = name;
    }

    /**
     * Determine if we happen to be within a method definition.
     * @return true if so
     */
    public boolean isWithinMethod() {
        for (StaticScope current = this; current != null; current = current.getEnclosingScope()) {
            if (current.getScopeType().isMethod()) return true;
        }

        return false;
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

    public void setFile(String file) {
        this.file = file;
    }

    public StaticScope duplicate() {
        StaticScope dupe = new StaticScope(type, enclosingScope, variableNames == null ? NO_NAMES : variableNames);
        // irScope is not guaranteed to be set onto StaticScope until it is executed for the first time.
        // We can call duplicate before its first execution.
        if (irScope != null) dupe.setIRScope(irScope);
        dupe.setScopeType(scopeType);
        dupe.setPreviousCRefScope(previousCRefScope);
        dupe.setModule(cref);
        dupe.setFile(file);
        dupe.setSignature(signature);
        dupe.firstKeywordIndex = firstKeywordIndex;

        return dupe;
    }

    public RubyModule getOverlayModuleForRead() {
        return overlayModule;
    }

    public RubyModule getOverlayModuleForWrite(ThreadContext context) {
        RubyModule omod = overlayModule;
        if (omod == null) {
            overlayModule = omod = defineModule(context);
        }
        return omod;
    }

    /**
     * Duplicate the parent scope's refinements overlay to get a moment-in-time snapshot.  Caller must
     * decide whether this scope is using (or maybe) using refinements.
     *
     * @param context
     */
    public void captureParentRefinements(ThreadContext context) {
        for (StaticScope cur = this.getEnclosingScope(); cur != null; cur = cur.getEnclosingScope()) {
            RubyModule overlay = cur.getOverlayModuleForRead();
            if (overlay != null && !overlay.getRefinements().isEmpty()) {
                // capture current refinements at definition time
                RubyModule myOverlay = getOverlayModuleForWrite(context);

                // FIXME: MRI does a copy-on-write thing here with the overlay
                myOverlay.getRefinementsForWrite().putAll(overlay.getRefinements());

                // only search until we find an overlay
                break;
            }
        }
    }

    public Collection<String> getInstanceVariableNames() {
        if (ivarNames != null) return ivarNames;

        if (irScope instanceof IRMethod) {
            return ivarNames = ((IRMethod) irScope).getMethodData().getIvarNames();
        }

        return ivarNames = Collections.EMPTY_LIST;
    }

    public void setInstanceVariableNames(Collection<String> ivarWrites) {
        this.ivarNames = ivarWrites;
    }

    public boolean isRuby2Keywords() {
        return irScope.isRuby2Keywords();
    }
}
