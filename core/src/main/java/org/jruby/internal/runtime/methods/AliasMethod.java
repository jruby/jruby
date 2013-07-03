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
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004-2005 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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
package org.jruby.internal.runtime.methods;

import org.jruby.RubyModule;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author  jpetersen
 */
public class AliasMethod extends DynamicMethod {
    private DynamicMethod oldMethod;
    private String oldName;

    public AliasMethod(RubyModule implementationClass, DynamicMethod oldMethod, String oldName) {
        super(implementationClass, oldMethod.getVisibility(), null);

        this.oldName = oldName;
        this.oldMethod = oldMethod;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name) {
        return oldMethod.call(context, self, klazz, oldName);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg) {
        return oldMethod.call(context, self, klazz, oldName, arg);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg1, IRubyObject arg2) {
        return oldMethod.call(context, self, klazz, oldName, arg1, arg2);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        return oldMethod.call(context, self, klazz, oldName, arg1, arg2, arg3);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject[] args) {
        return oldMethod.call(context, self, klazz, oldName, args);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, Block block) {
        return oldMethod.call(context, self, klazz, oldName, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg1, Block block) {
        return oldMethod.call(context, self, klazz, oldName, arg1, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg1, IRubyObject arg2, Block block) {
        return oldMethod.call(context, self, klazz, oldName, arg1, arg2, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        return oldMethod.call(context, self, klazz, oldName, arg1, arg2, arg3, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject[] args, Block block) {
        return oldMethod.call(context, self, klazz, oldName, args, block);
    }

    public DynamicMethod dup() {
        return new AliasMethod(implementationClass, oldMethod, oldName);
    }

    @Override
    public Arity getArity(){
        return oldMethod.getArity();
    }

    public String getOldName() {
        return oldName;
    }
    
    @Override
    public DynamicMethod getRealMethod() {
        return oldMethod.getRealMethod();
    }

    @Override
    public long getSerialNumber() {
        return oldMethod.getSerialNumber();
    }
    
    @Override
    public NativeCall getNativeCall(int args, boolean block) {
        return oldMethod.getNativeCall(args, block);
    }
    
    @Override
    public void setNativeCall(int args, boolean block, NativeCall nativeCall) {
        oldMethod.setNativeCall(args, block, nativeCall);
    }
    
    @Override
    public CallConfiguration getCallConfig() {
        return oldMethod.getCallConfig();
    }
}
