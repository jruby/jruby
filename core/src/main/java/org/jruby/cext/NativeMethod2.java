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
 * Copyright (C) 2009, 2010 Wayne Meissner
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

package org.jruby.cext;

import org.jruby.RubyModule;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * {@link NativeMethod2} represents an arity-2 native method. This class is for call optimization.
 */
public final class NativeMethod2 extends NativeMethod {
    public NativeMethod2(RubyModule clazz, int arity, long function) {
        super(clazz, arity, function);
    }

    @Override
    public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg0, IRubyObject arg1) {
        pre(context, self, getImplementationClass(), name);
        try {
            return getNativeInstance().callMethod2(function,
                    Handle.nativeHandle(self),
                    Handle.nativeHandle(arg0),
                    Handle.nativeHandle(arg1));
        } finally {
            post(context);
        }
    }

    @Override
    public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        pre(context, self, getImplementationClass(), name, block);
        try {
            return getNativeInstance().callMethod2(function,
                    Handle.nativeHandle(self),
                    Handle.nativeHandle(arg0),
                    Handle.nativeHandle(arg1));
        } finally {
            post(context);
        }
    }

}
