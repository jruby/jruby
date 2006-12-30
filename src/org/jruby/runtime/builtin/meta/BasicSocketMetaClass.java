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
 * Copyright (C) 2005 David Corbin <dcorbin@users.sourceforge.net>
 * Copyright (C) 2006 Evan Buswell <evan@heron.sytes.net>
 * Copyright (C) 2006 Thomas E Enebo <enebo@acm.org>
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
package org.jruby.runtime.builtin.meta;

import org.jruby.IRuby;
import org.jruby.RubyBasicSocket;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyBoolean;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.Arity;
import org.jruby.util.collections.SinglyLinkedList;

public class BasicSocketMetaClass extends IOMetaClass {

    public BasicSocketMetaClass(IRuby runtime) {
        super("BasicSocket", RubyBasicSocket.class, runtime.getClass("IO")); 
    }

    public BasicSocketMetaClass(String name, RubyClass superClass, SinglyLinkedList parentCRef) {
        this(name, RubyBasicSocket.class, superClass, parentCRef);
    }
    
    public BasicSocketMetaClass(String name, Class clazz, RubyClass superClass) {
    	super(name, clazz, superClass);
    }
    
    public BasicSocketMetaClass(String name, Class clazz, RubyClass superClass, SinglyLinkedList parentCRef) {
        super(name, clazz, superClass, parentCRef);
    }

    protected class BasicSocketMeta extends Meta {
    	protected void initializeClass() {
            // FIXME: shouldn't this be private?
            defineMethod("initialize", Arity.singleArgument());
            defineMethod("send", Arity.optional(), "write_send");
            defineMethod("recv", Arity.optional());
            defineMethod("shutdown", Arity.optional());
            defineMethod("__getsockname", Arity.noArguments(), "getsockname");
            defineMethod("__getpeername", Arity.noArguments(), "getpeername");
            defineSingletonMethod("do_not_reverse_lookup", Arity.noArguments());
            defineSingletonMethod("do_not_reverse_lookup=", Arity.singleArgument(), "set_do_not_reverse_lookup");
    	}
    };

    public IRubyObject do_not_reverse_lookup() {
        return getRuntime().newBoolean(getRuntime().isDoNotReverseLookupEnabled());
    }
    
    public IRubyObject set_do_not_reverse_lookup(IRubyObject flag) {
        getRuntime().setDoNotReverseLookupEnabled(((RubyBoolean) flag).isTrue());
        return getRuntime().newBoolean(getRuntime().isDoNotReverseLookupEnabled());
    }
    
    protected Meta getMeta() {
    	return new BasicSocketMeta();
    }

    public RubyClass newSubClass(String name, SinglyLinkedList parentCRef) {
        return new BasicSocketMetaClass(name, this, parentCRef);
    }

    public RubyClass newSubClass(String name, RubyModule parent) {
		BasicSocketMetaClass basicSocketMetaClass = new BasicSocketMetaClass(getRuntime());
        basicSocketMetaClass.initializeClass();
        
        return basicSocketMetaClass;
    }
    
    public IRubyObject allocateObject() {
        return new RubyBasicSocket(getRuntime(), this); 
    }
}
