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
 * Copyright (C) 2001 Chad Fowler <chadfowler@chadfowler.com>
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2005 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
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
package org.jruby;

import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.Frame;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author  jpetersen
 */
public class RubyBinding extends RubyObject {
    private Block block;

    public RubyBinding(Ruby runtime, RubyClass rubyClass, Block block) {
        super(runtime, rubyClass);
        
        this.block = block;
    }
    
    private static ObjectAllocator BINDING_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            RubyBinding instance = runtime.newBinding();
            
            instance.setMetaClass(klass);
            
            return instance;
        }
    };
    
    public static RubyClass createBindingClass(Ruby runtime) {
        RubyClass bindingClass = runtime.defineClass("Binding", runtime.getObject(), BINDING_ALLOCATOR);
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyBinding.class);   
        
        bindingClass.getMetaClass().defineMethod("of_caller", callbackFactory.getSingletonMethod("of_caller"));
        
        return bindingClass;
    }

    public Block getBlock() {
        return block;
    }

    // Proc class
    
    public static RubyBinding newBinding(Ruby runtime, Block block) {
        return new RubyBinding(runtime, runtime.getClass("Binding"), block);
    }

    public static RubyBinding newBinding(Ruby runtime) {
        ThreadContext context = runtime.getCurrentContext();
        
        // FIXME: We should be cloning, not reusing: frame, scope, dynvars, and potentially iter/block info
        Frame frame = context.getCurrentFrame();
        Block bindingBlock = Block.createBinding(frame, context.getCurrentScope());
        
        return new RubyBinding(runtime, runtime.getClass("Binding"), bindingBlock);
    }

    /**
     * Create a binding appropriate for a bare "eval", by using the previous (caller's) frame and current
     * scope.
     */
    public static RubyBinding newBindingForEval(Ruby runtime) {
        ThreadContext context = runtime.getCurrentContext();
        
        // This requires some explaining.  We use Frame values when executing blocks to fill in 
        // various values in ThreadContext and EvalState.eval like rubyClass, cref, and self.
        // Largely, for an eval that is using the logical binding at a place where the eval is 
        // called we mostly want to use the current frames value for this.  Most importantly, 
        // we need that self (JRUBY-858) at this point.  We also need to make sure that returns
        // jump to the right place (which happens to be the previous frame).  Lastly, we do not
        // want the current frames klazz since that will be the klazz represented of self.  We
        // want the class right before the eval (well we could use cref class for this too I think).
        // Once we end up having Frames created earlier I think the logic of stuff like this will
        // be better since we won't be worried about setting Frame to setup other variables/stacks
        // but just making sure Frame itself is correct...
        
        Frame previousFrame = context.getPreviousFrame();
        Frame currentFrame = context.getCurrentFrame();
        currentFrame.setKlazz(previousFrame.getKlazz());
        
        // Set jump target to whatever the previousTarget thinks is good.
        currentFrame.setJumpTarget(previousFrame.getJumpTarget() != null ? previousFrame.getJumpTarget() : previousFrame);
        
        Block bindingBlock = Block.createBinding(previousFrame, context.getCurrentScope());
        
        return new RubyBinding(runtime, runtime.getClass("Binding"), bindingBlock);
    }

    public static RubyBinding newBindingOfCaller(Ruby runtime) {
        ThreadContext context = runtime.getCurrentContext();
        
        // FIXME: We should be cloning, not reusing: frame, scope, dynvars, and potentially iter/block info
        Frame frame = context.getPreviousFrame();
        Block bindingBlock = Block.createBinding(frame, context.getPreviousScope());
        
        return new RubyBinding(runtime, runtime.getClass("Binding"), bindingBlock);
    }
    
    public static IRubyObject of_caller(IRubyObject recv, Block aBlock) {
        return RubyBinding.newBindingOfCaller(recv.getRuntime());
    }
}
