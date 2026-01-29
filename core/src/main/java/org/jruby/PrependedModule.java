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
 * Copyright (C) 2014 Timur Duehr <tduehr@gmail.com>
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.RefinedMarker;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * This class is used as an intermediate superclass for Module#prepend.  It takes over all
 * methods on the original module/class which is prepended and sets the originals methodLocation
 * to this class.  The orignial type no longer has methods so it will look down its inheritance
 * chain to find them.  The class which is actually prepended will be included onto the original
 * type.  This original method holding type will be put beneath the prepend module.
 *
 * @see org.jruby.IncludedModuleWrapper
 * @see org.jruby.RubyModule
 */
public class PrependedModule extends RubyClass implements DelegatedModule {
    private RubyModule origin;

    public PrependedModule(Ruby runtime, RubyClass superClass, RubyModule prependedClass) {
        super(runtime, superClass, false);
        origin = prependedClass;
        this.metaClass = origin.metaClass;
        if (superClass != null) {
            classIndex(superClass.getClassIndex()); // use same ClassIndex as metaclass, since we're technically still of that type
        }
        this.methods = prependedClass.getMethodsForWrite();
        prependedClass.methods = null;
        prependedClass.methodLocation = this;

        Map<String, DynamicMethod> methods = getMethods();
        for (Map.Entry<String, DynamicMethod> entry : methods.entrySet()) {
            DynamicMethod method = entry.getValue();
            if (moveRefinedMethod(entry.getKey(), method, prependedClass)) {
                methods.remove(entry.getKey());
            }
        }
    }

    /**
     * Transfer refined methods from the prepend stub to the origin as markers so they trigger refinements
     *
     * MRI: move_refined_method
     */
    private boolean moveRefinedMethod(String key, DynamicMethod method, RubyModule klass) {
        if (method.isRefined()) {
            if (method instanceof RefinedMarker) {
                // marker, add to actual class and remove from prepend
                klass.getMethodsForWrite().put(key, method);

                return true;
            } else {
                // real method as refinement, add marker to actual class
                klass.getMethodsForWrite().put(key, new RefinedMarker(klass, method.getVisibility(), method.getName()));

                return false;
            }
        }
        else {
            return false;
        }

    }

    @Override
    public boolean isPrepended() {
        return true;
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
    public boolean isImmediate() {
        return true;
    }

    @Override
    public void setMetaClass(RubyClass newRubyClass) {
        throw new UnsupportedOperationException("An included class is only a wrapper for a module");
    }

    @Override
    public String getName(ThreadContext context) {
        return origin.getName(context);
    }

    @Override
    public RubyModule getOrigin() {
        return origin;
    }

    @Override
    public RubyModule getDelegate() {
        return origin;
    }

    @Deprecated(since = "9.3.0.0")
    @Override
    public RubyModule getNonIncludedClass() {
        return origin;
    }

    @Deprecated(since = "10.0.3.0")
    @Override
    public IRubyObject id() {
        return origin.id();
    }

    /**
     * We don't want to reveal ourselves to Ruby code, so origin this
     * operation.
     */
    @Override
    public RubyInteger __id__(ThreadContext context) {
        return origin.__id__(context);
    }

    @Override
    public void addMethod(ThreadContext context, String id, DynamicMethod method) {
        super.addMethod(context, id, method);
        method.setDefinedClass(origin);
    }

    @Override
    protected synchronized Map<String, IRubyObject> getClassVariables() {
        return origin.getClassVariables();
    }

    @Override
    protected Map<String, IRubyObject> getClassVariablesForRead() {
        return origin.getClassVariablesForRead();
    }

    //
    // CONSTANT TABLE METHODS - pass to origin
    //

    @Override
    protected IRubyObject constantTableStore(String name, IRubyObject value) {
        // FIXME: legal here? may want UnsupportedOperationException
        return origin.constantTableStore(name, value);
    }

    protected IRubyObject constantTableStore(String name, IRubyObject value, boolean hidden) {
        // FIXME: legal here? may want UnsupportedOperationException
        return origin.constantTableStore(name, value, hidden);
    }

    @Override
    protected ConstantEntry constantEntryFetch(String name) {
        return origin.constantEntryFetch(name);
    }

    @Override
    protected IRubyObject constantTableRemove(String name) {
        // this _is_ legal (when removing an undef)
        return origin.constantTableRemove(name);
    }

    @Override
    protected IRubyObject getAutoloadConstant(ThreadContext context, String name, boolean forceLoad) {
        return origin.getAutoloadConstant(context, name, forceLoad);
    }

    @Override
    protected Map<String, Autoload> getAutoloadMap() {
        return origin.getAutoloadMap();
    }

    @Override
    protected Map<String, Autoload> getAutoloadMapForWrite() {
        return origin.getAutoloadMapForWrite();
    }

}
