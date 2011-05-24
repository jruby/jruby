/*
 **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2011 Charles O Nutter <headius@headius.com>
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
package org.jruby.ext.rubinius;

import java.io.IOException;
import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;
import static org.jruby.runtime.Visibility.*;

public class RubiniusLibrary implements Library {
    public void load(final Ruby runtime, boolean wrap) throws IOException {
        RubyModule rubinius = runtime.getOrCreateModule("Rubinius");
        RubyTuple.createTupleClass(runtime);

        final IRubyObject undefined = new RubyObject(runtime, runtime.getObject());
        runtime.getKernel().addMethod("undefined", new JavaMethod.JavaMethodZero(runtime.getKernel(), PRIVATE) {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                return undefined;
            }
        });

        // Toplevel "Ruby" module with some utility classes
        RubyModule rbxRuby = runtime.getOrCreateModule("Ruby");
        rbxRuby.defineAnnotatedMethods(RubiniusRuby.class);
        
        // Type module
        RubyModule type = runtime.defineModule("Type");
        type.defineAnnotatedMethods(RubiniusType.class);
        runtime.getLoadService().require("rubinius/kernel/common/type.rb");

        // LookupTable is just Hash for now
        rubinius.setConstant("LookupTable", runtime.getHash());
        
        // EnvironmentAccess
        RubyModule envAccess = rubinius.defineModuleUnder("EnvironmentAccess");
        envAccess.defineAnnotatedMethods(RubiniusEnvironmentAccess.class);
        runtime.getLoadService().require("rubinius/kernel/common/env.rb");
        
        // Thread-borne recursion detector stuff
        runtime.getLoadService().require("rubinius/kernel/common/thread.rb");
        
        // Extensions to Kernel
        runtime.getKernel().defineAnnotatedMethods(RubiniusKernel.class);
        
        // Channel class; we require bootstrap, overwrite it, and then require common
        runtime.getLoadService().require("rubinius/kernel/bootstrap/channel.rb");
        RubiniusChannel.createChannelClass(runtime);
        runtime.getLoadService().require("rubinius/kernel/common/channel.rb");
    }
}
