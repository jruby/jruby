package org.jruby;

import org.jruby.core.*;
import org.jruby.exceptions.*;
import org.jruby.runtime.*;

public class RubyProc extends RubyObject {
    // private originalThread = null

    private RubyBlock block = null;
    private RubyModule wrapper = null;

    public RubyProc(Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
    }
    
    public static RubyClass createProcClass(Ruby ruby) {
    	RubyClass procClass = ruby.defineClass("Proc", ruby.getClasses().getObjectClass());
    	
    	RubyCallbackMethod call = new ReflectionCallbackMethod(RubyProc.class, "call", true);
    	RubyCallbackMethod s_new = new ReflectionCallbackMethod(RubyProc.class, "s_new", RubyObject[].class, true, true);
    	
    	procClass.defineMethod("call", call);
    	
    	procClass.defineSingletonMethod("new", s_new);
    	
    	return procClass;
    }

	public static RubyProc s_new(Ruby ruby, RubyObject rubyClass, RubyObject[] args) {
		RubyProc proc = newProc(ruby, ruby.getClasses().getProcClass());
		
		proc.callInit(args);
		
		return proc;
	}

    public static RubyProc newProc(Ruby ruby, RubyClass rubyClass) {
        if (!ruby.isBlockGiven() && !ruby.isFBlockGiven()) {
            throw new RubyArgumentException(ruby, "tried to create Proc object without a block");
        }

        RubyProc newProc = new RubyProc(ruby, rubyClass);

        newProc.block = ruby.getBlock().cloneBlock();

        newProc.wrapper = ruby.getWrapper();
        newProc.block.iter = newProc.block.prev != null ? 1 : 0;

		newProc.block.frame = ruby.getRubyFrame();
		newProc.block.scope = ruby.getRubyScope();
        // +++

        return newProc;
    }

    public RubyObject call(RubyObject[] args) {
        RubyModule oldWrapper = getRuby().getWrapper();
        RubyBlock oldBlock = getRuby().getBlock();

        getRuby().setWrapper(wrapper);
        getRuby().setBlock(block);

        getRuby().getIter().push(RubyIter.ITER_CUR);
        getRuby().getRubyFrame().setIter(RubyIter.ITER_CUR);

        RubyObject result = getRuby().getNil();

        try {
            result = getRuby().yield0(args != null ? RubyArray.m_create(getRuby(), args) : null, null, null, true);
        } finally {
            getRuby().getIter().pop();
            getRuby().setBlock(oldBlock);
            getRuby().setWrapper(oldWrapper);
        }

        return result;
    }
}