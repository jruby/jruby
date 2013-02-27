/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2010 Charles O Nutter <headius@headius.com>
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
package org.jruby.ext.psych;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyException;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import static org.jruby.runtime.Visibility.*;

public class PsychToRuby {
    public static void initPsychToRuby(Ruby runtime, RubyModule psych) {
        RubyModule visitors = runtime.defineModuleUnder("Visitors", psych);
        RubyClass visitor = runtime.defineClassUnder("Visitor", runtime.getObject(), runtime.getObject().getAllocator(), visitors);
        RubyClass psychToRuby = runtime.defineClassUnder("ToRuby", visitor, RubyObject.OBJECT_ALLOCATOR, visitors);

        psychToRuby.defineAnnotatedMethods(PsychToRuby.class);
    }

    @JRubyMethod(visibility = PRIVATE)
    public static IRubyObject build_exception(ThreadContext context, IRubyObject self, IRubyObject klass, IRubyObject message) {
        if (klass instanceof RubyClass) {
            IRubyObject exception = ((RubyClass)klass).allocate();
            ((RubyException)exception).message = message;
            return exception;
        } else {
            throw context.runtime.newTypeError(klass, context.runtime.getClassClass());
        }
    }

    @JRubyMethod(visibility = PRIVATE)
    public static IRubyObject path2class(ThreadContext context, IRubyObject self, IRubyObject path) {
        try {
            return context.runtime.getClassFromPath(path.asJavaString());
        } catch (RaiseException re) {
            if (re.getException().getMetaClass() == context.runtime.getNameError()) {
                throw context.runtime.newArgumentError("undefined class/module " + path);
            }
            throw re;
        }
    }
}
