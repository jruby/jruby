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
 * Copyright (C) 2013 The JRuby Team (jruby@jruby.org)
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
package org.jruby.ext.ripper;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyRipper extends RubyObject {
    public static void initRipper(Ruby runtime) {
        RubyClass ripper = runtime.defineClass("Ripper", runtime.getObject(), new ObjectAllocator() {
            @Override
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                return new RubyRipper(runtime, klazz);
            }
        });
        
        ripper.defineAnnotatedMethods(RubyRipper.class);
    }

    private RubyRipper(Ruby runtime, RubyClass klazz) {
        super(runtime, klazz);
    }
    
    @JRubyMethod
    public IRubyObject parse(ThreadContext context) {
        return null;
    }
    
    @JRubyMethod
    public IRubyObject initialize(ThreadContext context, IRubyObject src) {
        return null;
    }

    @JRubyMethod
    public IRubyObject initialize(ThreadContext context, IRubyObject src, IRubyObject filename) {
        return null;
    }
    
    @JRubyMethod
    public IRubyObject initialize(ThreadContext context, IRubyObject src,IRubyObject filename, IRubyObject lineno) {
        return null;
    }
    
    @JRubyMethod
    public IRubyObject column(ThreadContext context) {
        return null;
    }

    @JRubyMethod
    public IRubyObject filename(ThreadContext context) {
        return null;
    }

    @JRubyMethod
    public IRubyObject lineno(ThreadContext context) {
        return null;
    }

    @JRubyMethod(name = "end_seen?")
    public IRubyObject end_seen_p(ThreadContext context) {
        return null;
    }

    @JRubyMethod
    public IRubyObject encoding(ThreadContext context) {
        return null;
    }

    @JRubyMethod
    public IRubyObject yydebug(ThreadContext context) {
        return null;
    }
    
    @JRubyMethod(name = "yydebug=")
    public IRubyObject yydebug_set(ThreadContext context, IRubyObject arg0) {
        return null;
    }    
}
