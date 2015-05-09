/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Visibility;
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
public class IncludedModuleWrapper extends IncludedModule {
    public IncludedModuleWrapper(Ruby runtime, RubyClass superClass, RubyModule origin) {
        super(runtime, superClass, origin);
        origin.addIncludingHierarchy(this);
        if (origin.methodLocation != origin) this.methodLocation = origin.methodLocation;
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
    public void addMethod(String name, DynamicMethod method) {
        throw new UnsupportedOperationException("An included class is only a wrapper for a module");
    }

    public void setMethods(Map newMethods) {
        throw new UnsupportedOperationException("An included class is only a wrapper for a module");
    }

    public RubyModule getDelegate() {
        return origin;
    }

    @Override
    public boolean isIncluded() {
        return true;
    }

    @Override
    public boolean isPrepended() {
        return origin.hasPrepends();
    }

    @Override
    protected boolean isSame(RubyModule module) {
        return origin.isSame(module.getDelegate());
    }

    @Override
    public Map<String, DynamicMethod> getMethods() {
        return origin.getMethods();
    }

    @Override
    public Map<String, DynamicMethod> getMethodsForWrite() {
        return origin.getMethodsForWrite();
    }

    @Override
    protected synchronized Map<String, IRubyObject> getClassVariables() {
        return origin.getClassVariables();
    }

    @Override
    protected Map<String, IRubyObject> getClassVariablesForRead() {
        return origin.getClassVariablesForRead();
    }

    @Override
    protected boolean variableTableContains(String name) {
        return origin.variableTableContains(name);
    }

    @Override
    protected Object variableTableFetch(String name) {
        return origin.variableTableFetch(name);
    }

    @Override
    protected Object variableTableStore(String name, Object value) {
        return origin.variableTableStore(name, value);
    }

    @Override
    protected Object variableTableRemove(String name) {
        return origin.variableTableRemove(name);
    }

    @Override
    protected void variableTableSync(List<Variable<Object>> vars) {
        origin.variableTableSync(vars);
    }

    //
    // CONSTANT TABLE METHODS - pass to origin
    //

    @Override
    protected boolean constantTableContains(String name) {
        return origin.constantTableContains(name);
    }

    @Override
    protected IRubyObject constantTableFetch(String name) {
        return origin.constantTableFetch(name);
    }

    @Override
    protected ConstantEntry constantEntryFetch(String name) {
        return origin.constantEntryFetch(name);
    }

    @Override
    protected IRubyObject constantTableStore(String name, IRubyObject value) {
        // FIXME: legal here? may want UnsupportedOperationException
        return origin.constantTableStore(name, value);
    }

    @Override
    protected IRubyObject constantTableRemove(String name) {
        // this _is_ legal (when removing an undef)
        return origin.constantTableRemove(name);
    }
    
    @Override
    @Deprecated
    public List<String> getStoredConstantNameList() {
        return origin.getStoredConstantNameList();
    }
    
    @Override
    public Collection<String> getConstantNames() {
        return origin.getConstantNames();
    }

    @Override
    public Collection<String> getConstantNames(boolean includePrivate) {
        return origin.getConstantNames(includePrivate);
    }

    @Override
    public IRubyObject getAutoloadConstant(String name) {
        return origin.getAutoloadConstant(name);
    }

    @Override
    protected DynamicMethod searchMethodCommon(String name) {
        // IncludedModuleWrapper needs to search prepended modules too, so search until we find methodLocation
        RubyModule module = origin;
        RubyModule methodLoc = origin.getMethodLocation();

        for (; module != methodLoc; module = module.getSuperClass()) {
            DynamicMethod method = module.getMethods().get(name);
            if (method != null) return method.isNull() ? null : method;
        }

        // one last search for method location
        DynamicMethod method = module.getMethods().get(name);
        if (method != null) return method.isNull() ? null : method;

        return null;
    }
}
