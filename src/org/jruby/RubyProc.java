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

import org.jruby.exceptions.JumpException;
import org.jruby.runtime.Block;
import org.jruby.runtime.Iter;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author  jpetersen
 */
public class RubyProc extends RubyObject {
    private Block block = null;
    private RubyModule wrapper = null;

    public RubyProc(IRuby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
    }

    public Block getBlock() {
        return block;
    }

    public RubyModule getWrapper() {
        return wrapper;
    }

    // Proc class

    public static RubyProc newProc(IRuby runtime, boolean isLambda) {
        ThreadContext tc = runtime.getCurrentContext();
        
        if (!tc.isBlockGiven() && !tc.isFBlockGiven()) {
            throw runtime.newArgumentError("tried to create Proc object without a block");
        }
        
        if (isLambda && !tc.isBlockGiven()) {
        	// TODO: warn "tried to create Proc object without a block"
        }
        
        Block block = (Block) tc.getCurrentBlock();
        
        if (!isLambda && block.getBlockObject() instanceof RubyProc) {
        	return (RubyProc) block.getBlockObject();
        }

        RubyProc newProc = new RubyProc(runtime, runtime.getClass("Proc"));

        newProc.block = block.cloneBlock();
        newProc.wrapper = tc.getWrapper();
        newProc.block.setIter(newProc.block.getNext() != null ? Iter.ITER_PRE : Iter.ITER_NOT);
        newProc.block.isLambda = isLambda;
        block.setBlockObject(newProc);

        return newProc;
    }
    
    protected IRubyObject doClone() {
    	RubyProc newProc = 
    		new RubyProc(getRuntime(), getRuntime().getClass("Proc"));
    	
    	newProc.block = getBlock();
    	newProc.wrapper = getWrapper();
    	
    	return newProc;
    }
    
    public IRubyObject binding() {
        return getRuntime().newBinding(block);
    }
    
    public IRubyObject call(IRubyObject[] args) {
        return call(args, null);
    }

    public IRubyObject call(IRubyObject[] args, IRubyObject self) {
    	assert args != null;
    	
        ThreadContext context = getRuntime().getCurrentContext();
        RubyModule oldWrapper = context.getWrapper();
        context.setWrapper(wrapper);
        try {
        	if (block.isLambda) {
        		block.arity().checkArity(getRuntime(), args);
        	}
        	
        	return block.call(args, self);
        } catch (JumpException je) {
        	if (je.getJumpType() == JumpException.JumpType.BreakJump) {
        		if (block.isLambda) {
	                return (IRubyObject)je.getPrimaryData();
	            } 
		        throw getRuntime().newLocalJumpError("unexpected return");
        	} else if (je.getJumpType() == JumpException.JumpType.ReturnJump) {
        		Object target = je.getPrimaryData();
	
	            if (target == this || block.isLambda) {
	                return (IRubyObject)je.getSecondaryData();
	            } else if (target == null) {
	            	throw getRuntime().newLocalJumpError("unexpected return");
	            }
	            throw je;
        	} else {
        		throw je;
        	}
        } finally {
            context.setWrapper(oldWrapper);
        }
    }

    public RubyFixnum arity() {
        return getRuntime().newFixnum(block.arity().getValue());
    }
    
    public RubyProc to_proc() {
    	return this;
    }
}
