/*
 **** BEGIN LICENSE BLOCK *****
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
import org.jruby.runtime.callsite.CacheEntry;

/**
 * Represents a method which has been aliased.
 *
 * Aliased methods pass as frame class the implementationClass they were created with, which should reflect
 * the level in the related class hierarchy where the alias appears. This allows aliased methods that super to do
 * so from the appropriate starting level.
 */
public class AliasMethod extends DynamicMethod {
    private CacheEntry entry;

    /**
     * For some java native methods it is convenient to pass in a String instead
     * of a ByteList.
     */ 
    public AliasMethod(RubyModule implementationClass, CacheEntry entry, String oldName) {
        super(implementationClass, entry.method.getVisibility(), oldName);

        this.entry = entry;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String unused) {
        return entry.method.call(context, self, entry.sourceModule, name);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String unused, IRubyObject arg) {
        return entry.method.call(context, self, entry.sourceModule, name, arg);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String unused, IRubyObject arg1, IRubyObject arg2) {
        return entry.method.call(context, self, entry.sourceModule, name, arg1, arg2);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String unused, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        return entry.method.call(context, self, entry.sourceModule, name, arg1, arg2, arg3);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String unused, IRubyObject[] args) {
        return entry.method.call(context, self, entry.sourceModule, name, args);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String unused, Block block) {
        return entry.method.call(context, self, entry.sourceModule, name, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String unused, IRubyObject arg1, Block block) {
        return entry.method.call(context, self, entry.sourceModule, name, arg1, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String unused, IRubyObject arg1, IRubyObject arg2, Block block) {
        return entry.method.call(context, self, entry.sourceModule, name, arg1, arg2, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String unused, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        return entry.method.call(context, self, entry.sourceModule, name, arg1, arg2, arg3, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String unused, IRubyObject[] args, Block block) {
        return entry.method.call(context, self, entry.sourceModule, name, args, block);
    }

    public DynamicMethod dup() {
        return new AliasMethod(implementationClass, entry, name);
    }

    @Override
    public Arity getArity(){
        return entry.method.getArity();
    }

    public String getOldName() {
        return entry.method.getName();
    }
    
    @Override
    public DynamicMethod getRealMethod() {
        return entry.method.getRealMethod();
    }

    @Override
    public long getSerialNumber() {
        return entry.method.getSerialNumber();
    }

}
