/*
 * RubyProc.java - No description
 * Created on 18.01.2002, 01:04:33
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina, Chad Fowler
 * Copyright (C) 2002-2004 Thomas E Enebo
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Chad Fowler <chadfowler@yahoo.com>
 * Thomas E Enebo <enebo@acm.org>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */
package org.jruby;

import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.ReturnJump;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.Iter;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author  jpetersen
 * @version $Revision$
 */
public class RubyProc extends RubyObject {
    private Block block = null;
    private RubyModule wrapper = null;

    public RubyProc(Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
    }

    public static RubyClass createProcClass(Ruby ruby) {
        RubyClass result = ruby.defineClass("Proc", 
                ruby.getClasses().getObjectClass());
        CallbackFactory callbackFactory = ruby.callbackFactory();
        
        result.defineMethod("arity", 
                callbackFactory.getMethod(RubyProc.class, "arity"));
        result.defineMethod("call", 
                callbackFactory.getOptMethod(RubyProc.class, "call"));
        result.defineMethod("[]", 
                callbackFactory.getOptMethod(RubyProc.class, "call"));

        result.defineSingletonMethod("new", 
                callbackFactory.getOptSingletonMethod(RubyProc.class, "newInstance"));
        
        return result;
    }

    public Block getBlock() {
        return block;
    }

    public RubyModule getWrapper() {
        return wrapper;
    }

    // Proc class

    public static RubyProc newInstance(IRubyObject receiver, IRubyObject[] args) {
        RubyProc proc = newProc(receiver.getRuntime());
        proc.callInit(args);
        return proc;
    }

    public static RubyProc newProc(Ruby ruby, boolean isLambda) {
        if (!ruby.isBlockGiven() && !ruby.isFBlockGiven()) {
            throw new ArgumentError(ruby, "tried to create Proc object without a block");
        }

        RubyProc newProc = new RubyProc(ruby, ruby.getClasses().getProcClass());

        newProc.block = ruby.getBlockStack().getCurrent().cloneBlock();
        newProc.wrapper = ruby.getWrapper();
        newProc.block.setIter(newProc.block.getNext() != null ? Iter.ITER_PRE : Iter.ITER_NOT);
        newProc.block.isLambda = isLambda;

        return newProc;
    }
    
    public static RubyProc newProc(Ruby ruby) {
    	return newProc(ruby, false);
    }

    public IRubyObject call(IRubyObject[] args) {
        return call(args, null);
    }

    public IRubyObject call(IRubyObject[] args, IRubyObject self) {
        ThreadContext context = getRuntime().getCurrentContext();
        RubyModule oldWrapper = context.getWrapper();
        context.setWrapper(wrapper);
        try {
            return block.call(args, self);
        } catch (ReturnJump e) {
        	if (block.isLambda) {
        		return e.getReturnValue();
        	}
        	
        	if (context.getFrameStack().getPrevious() == block.getFrame()) {
        			throw e;
	        }
        	
		  	throw new RaiseException(getRuntime(), 
		  			getRuntime().getExceptions().getLocalJumpError(), "unexpected return");
        } finally {
            context.setWrapper(oldWrapper);
        }
    }

    public RubyFixnum arity() {
        return RubyFixnum.newFixnum(runtime, block.arity().getValue());
    }
}
