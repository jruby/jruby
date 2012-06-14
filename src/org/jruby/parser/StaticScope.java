/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.parser;

import java.io.Serializable;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.ast.AssignableNode;
import org.jruby.ast.Node;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Arity;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.scope.DummyDynamicScope;

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
public abstract class StaticScope implements Serializable {
    private static final long serialVersionUID = 4843861446986961013L;
    
    // Next immediate scope.  Variable and constant scoping rules make use of this variable
    // in different ways.
    final protected StaticScope enclosingScope;
    
    // Live reference to module
    private transient RubyModule cref = null;
    
    // Next CRef down the lexical structure
    private StaticScope previousCRefScope = null;
    
    // Our name holder (offsets are assigned as variables are added
    private String[] variableNames;
    
    // number of variables in this scope representing required arguments
    private int requiredArgs = 0;
    
    // number of variables in this scope representing optional arguments
    private int optionalArgs = 0;
    
    // index of variable that represents a "rest" arg
    private int restArg = -1;

    private boolean isBackrefLastlineScope = false;
    
    private DynamicScope dummyScope;

    /**
     * Construct a new static scope. The array of strings should all be the
     * interned versions, since several other optimizations depend on being
     * able to do object equality checks.
     *
     * @param enclosingScope The lexically containing scope.
     * @param names The list of interned String variable names.
     */
    protected StaticScope(StaticScope enclosingScope, String[] names) {
        assert names != null : "names is not null";
        assert namesAreInterned(names);
        
        this.enclosingScope = enclosingScope;
        this.variableNames = names;
    }

    /**
     * Check that all strings in the given array are the interned versions.
     *
     * @param names The array of strings
     * @return true if they are all interned, false otherwise
     */
    private static boolean namesAreInterned(String[] names) {
        for (String name : names) {
            // Note that this object equality check is intentional, to ensure
            // the string and its interned version are the same object.
            if (name != name.intern()) return false;
        }
        return true;
    }

    /**
     * Add a new variable to this (current) scope unless it is already defined in the
     * current scope.
     *
     * @param name of new variable
     * @return index+depth merged location of scope
     */
    public int addVariableThisScope(String name) {
        int slot = exists(name);

        if (slot >= 0) return slot;

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
            
        // This is perhaps innefficient timewise?  Optimal spacewise
        growVariableNames(name);
        
        // Returns slot of variable
        return variableNames.length - 1;
    }
    
    public String[] getVariables() {
        return variableNames;
    }
    
    public int getNumberOfVariables() {
        return variableNames.length;
    }
    
    public void setVariables(String[] names) {
        assert names != null : "names is not null";
        
        variableNames = new String[names.length];
        System.arraycopy(names, 0, variableNames, 0, names.length);
    }

    /* Note: Only used by compiler until it can use getConstant again or use some other refactoring */
    public IRubyObject getConstantWithConstMissing(Ruby runtime, String internedName, RubyModule object) {
        IRubyObject result = getConstantInner(runtime, internedName, object);

        // If we could not find the constant from cref..then try getting from inheritence hierarchy
        return result == null ? cref.getConstant(internedName) : result;        
    }
    
    public IRubyObject getConstant(Ruby runtime, String internedName, RubyModule object) {
        IRubyObject result = getConstantInner(runtime, internedName, object);

        // If we could not find the constant from cref..then try getting from inheritence hierarchy
        return result == null ? cref.getConstantNoConstMissing(internedName) : result;
    }

    public IRubyObject getConstantInner(Ruby runtime, String internedName, RubyModule object) {
        IRubyObject result = cref.fetchConstant(internedName);

        if (result != null) {
            return result == RubyObject.UNDEF ? cref.resolveUndefConstant(runtime, internedName) : result;
        }

        return previousCRefScope == null ? null : previousCRefScope.getConstantInnerNoObject(runtime, internedName, object);
    }
    
    private IRubyObject getConstantInnerNoObject(Ruby runtime, String internedName, RubyModule object) {
        if (previousCRefScope == null) return null;

        return getConstantInner(runtime, internedName, object);
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
    public int exists(String name) {
        return findVariableName(name);
    }
    
    private int findVariableName(String name) {
        for (int i = 0; i < variableNames.length; i++) {
            if (name == variableNames[i]) return i;
        }
        return -1;
    }
    
    /**
     * Is this name in the visible to the current scope
     * 
     * @param name to be looked for
     * @return a location where the left-most 16 bits of number of scopes down it is and the
     *   right-most 16 bits represents its index in that scope
     */
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
    public AssignableNode assign(ISourcePosition position, String name, Node value) {
        return assign(position, name, value, this, 0);
    }
    
    /**
     * Get all visible variables that we can see from this scope that have been assigned
     * (e.g. seen so far)
     * 
     * @return a list of all names (sans $~ and $_ which are special names)
     */
    public abstract String[] getAllNamesInScope();
    
    protected abstract int isDefined(String name, int depth);
    protected abstract AssignableNode assign(ISourcePosition position, String name, Node value, 
            StaticScope topScope, int depth);
    protected abstract Node declare(ISourcePosition position, String name, int depth);
    
    /**
     * Make a DVar or LocalVar node based on scoping logic
     * 
     * @param position the location that in the source that the new node will come from
     * @param name of the variable to be created is named
     * @return a DVarNode or LocalVarNode
     */
    public Node declare(ISourcePosition position, String name) {
        return declare(position, name, 0);
    }

    /**
     * Gets the Local Scope relative to the current Scope.  For LocalScopes this will be itself.
     * Blocks will contain the LocalScope it contains.
     * 
     * @return localScope
     */
    public abstract StaticScope getLocalScope();
    
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

    public int getOptionalArgs() {
        return optionalArgs;
    }

    public int getRequiredArgs() {
        return requiredArgs;
    }

    public void setRequiredArgs(int requiredArgs) {
        this.requiredArgs = requiredArgs;
    }

    public int getRestArg() {
        return restArg;
    }

    public void setRestArg(int restArg) {
        this.restArg = restArg;
    }

    /**
     * Argument scopes represent scopes which contain arguments for zsuper.  All LocalStaticScopes
     * are argument scopes and BlockStaticScopes can be when they are used by define_method.
     */
    public abstract boolean isArgumentScope();
    public abstract void makeArgumentScope();

    public boolean isBlockScope() {
        return false;
    }

    public boolean isBackrefLastlineScope() {
        return isBackrefLastlineScope;
    }

    public void setBackrefLastlineScope(boolean isBackrefLastlineScope) {
        this.isBackrefLastlineScope = isBackrefLastlineScope;
    }
    
    public Arity getArity() {
        if (optionalArgs > 0) {
            if (restArg >= 0) {
                return Arity.optional();
            }
            return Arity.required(requiredArgs);
        } else {
            if (restArg >= 0) {
                return Arity.optional();
            }
            return Arity.fixed(requiredArgs);
        }
    }
    
    public void setArities(int required, int optional, int rest) {
        this.requiredArgs = required;
        this.optionalArgs = optional;
        this.restArg = rest;
    }
    
    public DynamicScope getDummyScope() {
        return dummyScope == null ? dummyScope = new DummyDynamicScope(this) : dummyScope;
    }

    private void growVariableNames(String name) {
        String[] newVariableNames = new String[variableNames.length + 1];
        System.arraycopy(variableNames, 0, newVariableNames, 0, variableNames.length);
        variableNames = newVariableNames;
        variableNames[variableNames.length - 1] = name;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("[");
            
        for (int i = 0; i < variableNames.length - 1; i++) {
            buf.append(variableNames[i]).append(", ");
        }
        if (variableNames.length > 0) {
            buf.append(variableNames[variableNames.length - 1]);
        }
        buf.append("]");
        buf.append(", cref = ").append(cref);
            
        return buf.toString();
    }
}
