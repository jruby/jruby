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
 * Copyright (C) 2005 Thomas E Enebo <enebo@acm.org>
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
import org.jruby.RubyClass;
import org.jruby.RubyProc;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.collections.SinglyLinkedList;

public class ProcMetaClass extends ObjectMetaClass {
    public ProcMetaClass(IRuby runtime) {
        super("Proc", RubyProc.class, runtime.getObject(), PROC_ALLOCATOR);
    }
    
	public ProcMetaClass(String name, RubyClass superClass, ObjectAllocator allocator, SinglyLinkedList parentCRef) {
		super(name, RubyProc.class, superClass, allocator, parentCRef);
	}

	protected class ProcMeta extends Meta {
		protected void initializeClass() {
			defineFastMethod("arity", Arity.noArguments(), "arity");
			defineFastMethod("binding", Arity.noArguments(), "binding");
			defineMethod("call", Arity.optional(), "call");
			defineAlias("[]", "call");
			defineFastMethod("to_proc", Arity.noArguments(), "to_proc");
            defineMethod("initialize", Arity.optional());

            defineSingletonMethod("new", Arity.optional(), "newInstance");
        }
	};
	
	protected Meta getMeta() {
		return new ProcMeta();
	}
    
    /**
     * Create a new instance of a Proc object.  We override this method (from RubyClass)
     * since we need to deal with special case of Proc.new with no arguments or block arg.  In 
     * this case, we need to check previous frame for a block to consume.
     */
    public IRubyObject newInstance(IRubyObject[] args, Block block) {
        IRubyObject obj = (IRubyObject) allocate();
        
        // No passed in block, lets check next outer frame for one ('Proc.new')
        if (block == null) {
            block = getRuntime().getCurrentContext().getPreviousFrame().getBlock();
        }
        
        obj.callInit(args, block);
        return obj;
    }
    
	public RubyClass newSubClass(String name, SinglyLinkedList parentCRef) {
		return new ProcMetaClass(name, this, PROC_ALLOCATOR, parentCRef);
	}
    
    private static ObjectAllocator PROC_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(IRuby runtime, RubyClass klass) {
            RubyProc instance = RubyProc.newProc(runtime, false);

            instance.setMetaClass(klass);

            return instance;
        }
    };
}
