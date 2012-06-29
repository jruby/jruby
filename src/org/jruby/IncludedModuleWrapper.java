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
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004-2006 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
 * Copyright (C) 2007 William N Dortch <bill.dortch@gmail.com>
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
package org.jruby;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.Variable;

/**
 * This class is used to provide an intermediate superclass for modules and classes that include
 * other modules. It inserts itself as the immediate superClass of the includer, but defers all
 * module methods to the actual superclass. Multiple of these intermediate superclasses can be
 * added for multiple included modules.
 * 
 * This allows the normal superclass-based searches (searchMethod, getConstant, etc) to traverse
 * the superclass ancestors as normal while the included modules do not actually show up in
 * direct inheritance traversal.
 * 
 * @see org.jruby.RubyModule
 */
public class IncludedModuleWrapper extends RubyClass {
    private final RubyModule delegate;

    public IncludedModuleWrapper(Ruby runtime, RubyClass superClass, RubyModule delegate) {
        super(runtime, superClass, false);
        this.delegate = delegate;
        this.metaClass = delegate.metaClass;
        delegate.addIncludingHierarchy(this);
    }

    /**
     * Overridden newIncludeClass implementation to allow attaching future includes to the correct module
     * (i.e. the one to which this is attached)
     * 
     * @see org.jruby.RubyModule#newIncludeClass(RubyClass)
     */
    @Override
    @Deprecated
    public IncludedModuleWrapper newIncludeClass(RubyClass superClass) {
        IncludedModuleWrapper includedModule = new IncludedModuleWrapper(getRuntime(), superClass, getNonIncludedClass());
        
        // include its parent (and in turn that module's parents)
        if (getSuperClass() != null) {
            includedModule.includeModule(getSuperClass());
        }
        
        return includedModule;
    }

    @Override
    public boolean isModule() {
        return false;
    }

    @Override
    public boolean isClass() {
        return false;
    }

    @Override
    public boolean isIncluded() {
        return true;
    }
    
    @Override
    public boolean isImmediate() {
        return true;
    }

    @Override
    public void setMetaClass(RubyClass newRubyClass) {
        throw new UnsupportedOperationException("An included class is only a wrapper for a module");
    }

    @Override
    public Map<String, DynamicMethod> getMethods() {
        return delegate.getMethods();
    }

    @Override
    public Map<String, DynamicMethod> getMethodsForWrite() {
        return delegate.getMethodsForWrite();
    }

    @Override
    public void addMethod(String name, DynamicMethod method) {
        throw new UnsupportedOperationException("An included class is only a wrapper for a module");
    }

    public void setMethods(Map newMethods) {
        throw new UnsupportedOperationException("An included class is only a wrapper for a module");
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public RubyModule getNonIncludedClass() {
        return delegate;
    }
    
    @Override
    public RubyClass getRealClass() {
        if (superClass == null) return null;
        return superClass.getRealClass();
    }

    @Override
    protected boolean isSame(RubyModule module) {
        return delegate.isSame(module);
    }
    
   /**
    * We don't want to reveal ourselves to Ruby code, so delegate this
    * operation.
    */    
    @Override
    public IRubyObject id() {
        return delegate.id();
    }

    @Override
    protected synchronized Map<String, IRubyObject> getClassVariables() {
        return delegate.getClassVariables();
    }

    @Override
    protected Map<String, IRubyObject> getClassVariablesForRead() {
        return delegate.getClassVariablesForRead();
    }

    @Override
    protected boolean variableTableContains(String name) {
        return delegate.variableTableContains(name);
    }

    @Override
    protected Object variableTableFetch(String name) {
        return delegate.variableTableFetch(name);
    }

    @Override
    protected Object variableTableStore(String name, Object value) {
        return delegate.variableTableStore(name, value);
    }

    @Override
    protected Object variableTableRemove(String name) {
        return delegate.variableTableRemove(name);
    }

    @Override
    protected void variableTableSync(List<Variable<Object>> vars) {
        delegate.variableTableSync(vars);
    }

    //
    // CONSTANT TABLE METHODS - pass to delegate
    //

    @Override
    protected boolean constantTableContains(String name) {
        return delegate.constantTableContains(name);
    }

    @Override
    protected IRubyObject constantTableFetch(String name) {
        return delegate.constantTableFetch(name);
    }

    @Override
    protected ConstantEntry constantEntryFetch(String name) {
        return delegate.constantEntryFetch(name);
    }

    @Override
    protected IRubyObject constantTableStore(String name, IRubyObject value) {
        // FIXME: legal here? may want UnsupportedOperationException
        return delegate.constantTableStore(name, value);
    }

    @Override
    protected IRubyObject constantTableRemove(String name) {
        // this _is_ legal (when removing an undef)
        return delegate.constantTableRemove(name);
    }
    
    @Override
    @Deprecated
    public List<String> getStoredConstantNameList() {
        return delegate.getStoredConstantNameList();
    }
    
    @Override
    public Collection<String> getConstantNames() {
        return delegate.getConstantNames();
    }

    @Override
    public Collection<String> getConstantNames(boolean includePrivate) {
        return delegate.getConstantNames(includePrivate);
    }

    @Override
    public IRubyObject getAutoloadConstant(Ruby runtime, String name) {
        return delegate.getAutoloadConstant(runtime, name);
    }
}
