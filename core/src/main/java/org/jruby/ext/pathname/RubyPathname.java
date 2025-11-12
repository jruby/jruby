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
 * Copyright (C) 2013 Benoit Daloze <eregontp@gmail.com>
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

package org.jruby.ext.pathname;

import static org.jruby.anno.FrameField.BACKREF;
import static org.jruby.api.Access.*;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.asSymbol;
import static org.jruby.api.Create.*;
import static org.jruby.api.Error.argumentError;

import org.jruby.*;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.api.Access;
import org.jruby.api.Define;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.ivars.VariableTableManager;

import java.lang.invoke.MethodHandles;

@JRubyClass(name = "Pathname")
public class RubyPathname extends RubyObject {
    // assigned from Ruby
    private RubyString path;

    public static RubyClass createPathnameClass(ThreadContext context) {
        RubyClass Pathname = Define.
                defineClass(context, "Pathname", objectClass(context), RubyPathname::new).
                defineMethods(context, RubyPathname.class);

        MethodHandles.Lookup lookup = MethodHandles.lookup();
        VariableTableManager vtm = Pathname.getVariableTableManager();
        try {
            vtm.getVariableAccessorForRubyVar("@path",
                    lookup.findGetter(RubyPathname.class, "path", RubyString.class),
                    lookup.findSetter(RubyPathname.class, "path", RubyString.class));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // should not happen
            throw new RuntimeException(e);
        }

        return Pathname;
    }

    public RubyPathname(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    public static RubyPathname newInstance(ThreadContext context, RubyClass klass, IRubyObject path) {
        RubyPathname pathname = new RubyPathname(context.runtime, klass);
        pathname.callInit(context, path, Block.NULL_BLOCK);
        return pathname;
    }

    public static RubyPathname newInstance(ThreadContext context, IRubyObject path) {
        return newInstance(context, Access.getClass(context, "Pathname"), path);
    }

    private int cmp(RubyPathname other) {
        byte[] a = path.getByteList().bytes();
        byte[] b = other.path.getByteList().bytes();
        int i;
        for (i = 0; i < a.length && i < b.length; i++) {
            byte ca = a[i];
            byte cb = b[i];
            if (ca == '/') {
                ca = '\0';
            }
            if (cb == '/') {
                cb = '\0';
            }
            if (ca != cb) {
                return ca < cb ? -1 : 1;
            }
        }
        if (i < a.length) {
            return 1;
        }
        if (i < b.length) {
            return -1;
        }
        return 0;
    }

    @Override
    @JRubyMethod(name = "<=>")
    public IRubyObject op_cmp(ThreadContext context, IRubyObject other) {
        return other instanceof RubyPathname path ?
                asFixnum(context, cmp(path)) :
                context.nil;
    }

    @JRubyMethod(writes = BACKREF)
    public IRubyObject sub(ThreadContext context, IRubyObject arg0, Block block) {
        return newInstance(context, path.sub(context, arg0, block));
    }

    @JRubyMethod(writes = BACKREF)
    public IRubyObject sub(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        return newInstance(context, path.sub(context, arg0, arg1, block));
    }
}
