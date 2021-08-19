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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.RefinedMarker;

/**
 * This class is used as an intermediate superclass for Module#prepend
 *
 * @see org.jruby.IncludedModuleWrapper
 * @see org.jruby.RubyModule
 */
public class PrependedModule extends IncludedModule {

    public PrependedModule(Ruby runtime, RubyClass superClass, RubyModule klass) {
        super(runtime, superClass, klass);
        this.methods = klass.methods;
        klass.methods = Collections.EMPTY_MAP;
        klass.methodLocation = this;
        for (Map.Entry<String, DynamicMethod> entry : methods.entrySet()) {
            DynamicMethod method = entry.getValue();
            if (moveRefinedMethod(entry.getKey(), method, klass)) {
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
    public void addMethod(String id, DynamicMethod method) {
        super.addMethod(id, method);
        method.setDefinedClass(origin);
    }

    @Override
    public boolean isPrepended() {
        return true;
    }

}
