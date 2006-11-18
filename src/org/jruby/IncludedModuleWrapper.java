/***** BEGIN LICENSE BLOCK *****
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

import java.util.Map;

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
public final class IncludedModuleWrapper extends RubyClass {
    private RubyModule delegate;

    public IncludedModuleWrapper(IRuby runtime, RubyClass superClass, RubyModule delegate) {
        super(runtime, superClass);

        this.delegate = delegate;
    }

    /**
     * Overridden newIncludeClass implementation to allow attaching future includes to the correct module
     * (i.e. the one to which this is attached)
     * 
     * @see org.jruby.RubyModule#newIncludeClass(RubyClass)
     */
    public IncludedModuleWrapper newIncludeClass(RubyClass superClass) {
        IncludedModuleWrapper includedModule = new IncludedModuleWrapper(getRuntime(), superClass, getNonIncludedClass());
        
        // include its parent (and in turn that module's parents)
        if (getSuperClass() != null) {
            includedModule.includeModule(getSuperClass());
        }
        
        return includedModule;
    }

    public boolean isModule() {
        return false;
    }

    public boolean isClass() {
        return false;
    }

    public boolean isIncluded() {
        return true;
    }
    
    public boolean isImmediate() {
        return true;
    }

    public RubyClass getMetaClass() {
		return delegate.getMetaClass();
    }

    public void setMetaClass(RubyClass newRubyClass) {
        throw new UnsupportedOperationException("An included class is only a wrapper for a module");
    }

    public Map getMethods() {
        return delegate.getMethods();
    }

    public void setMethods(Map newMethods) {
        throw new UnsupportedOperationException("An included class is only a wrapper for a module");
    }

    public Map getInstanceVariables() {
        return delegate.getInstanceVariables();
    }

    public void setInstanceVariables(Map newMethods) {
        throw new UnsupportedOperationException("An included class is only a wrapper for a module");
    }

    public String getName() {
		return delegate.getName();
    }

    public RubyModule getNonIncludedClass() {
        return delegate;
    }
    
    public RubyClass getRealClass() {
        return getSuperClass().getRealClass();
    }

    public boolean isSame(RubyModule module) {
        return delegate.isSame(module);
    }
    
   /**
    * We don't want to reveal ourselves to Ruby code, so delegate this
    * operation.
    */    
    public RubyFixnum id() {
        return delegate.id();
    }
}
