/*
 * RubyBlock.java - No description
 * Created on 20. September 2001, 18:15
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
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

package org.jruby.runtime;

import org.jruby.*;
import org.jruby.nodes.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class RubyBlock {
    public static final int BLOCK_D_SCOPE  = 1;
    public static final int BLOCK_DYNAMIC  = 2;
    public static final int BLOCK_ORPHAN   = 4;
    
    public Node var;
    public Node body;
    public RubyObject self;
    public RubyFrame frame;
    public RubyScope scope;
    // private BLOCKTAG tag;
    public RubyModule klass;
    public int iter;
    public int vmode;
    public int flags;
    public RubyVarmap dynamicVars;
    public RubyObject origThread;
    public RubyBlock prev;
    
    private Ruby ruby;

    public RubyBlock(Ruby ruby) {
        this.ruby = ruby;
    }
    
    protected RubyBlock(Node var, Node body, RubyObject self, RubyFrame frame, RubyScope scope, 
                        RubyModule klass, int iter, int vmode, int flags, RubyVarmap dynamicVars,
                        RubyObject origThread, RubyBlock prev, Ruby ruby) {
        this(ruby);
                            
        this.var = var;
        this.body = body;
        this.self = self;
        this.frame = frame;
        this.scope = scope;
        this.klass = klass;
        this.iter = iter;
        this.vmode = vmode;
        this.flags = flags;
        this.dynamicVars = dynamicVars;
        this.origThread = origThread;
        this.prev = prev;
    }

    public void push(Node v, Node b, RubyObject newSelf) {
        RubyBlock oldBlock = new RubyBlock(var, body, self, frame, scope, klass,
                                           iter, vmode, flags, dynamicVars,
                                           origThread, prev, ruby);
        
        var = v;
        body = b;
        self = newSelf;
        frame = ruby.getRubyFrame();
        klass = ruby.getRubyClass();
        frame.setFile(ruby.getSourceFile());
        frame.setLine(ruby.getSourceLine());
        scope = ruby.getRubyScope();
        prev = oldBlock;
        iter = ruby.getIter().getIter();
        vmode = ruby.getActMethodScope();
        flags = BLOCK_D_SCOPE;
        dynamicVars = ruby.getDynamicVars();
    }

    public void pop() {
        this.var = prev.var;
        this.body = prev.body;
        this.self = prev.self;
        this.frame = prev.frame;
        this.scope = prev.scope;
        this.klass = prev.klass;
        this.iter = prev.iter;
        this.vmode = prev.vmode;
        this.flags = prev.flags;
        this.dynamicVars = prev.dynamicVars;
        this.origThread = prev.origThread;
        this.prev = prev.prev;
    }
    
    public RubyBlock getTmp() {
        return new RubyBlock(var, body, self, frame, scope, klass,
                                           iter, vmode, flags, dynamicVars,
                                           origThread, prev, ruby);
    }

    public void setTmp(RubyBlock block) {
        this.var = block.var;
        this.body = block.body;
        this.self = block.self;
        this.frame = block.frame;
        this.scope = block.scope;
        this.klass = block.klass;
        this.iter = block.iter;
        this.vmode = block.vmode;
        this.flags = block.flags;
        this.dynamicVars = block.dynamicVars;
        this.origThread = block.origThread;
        this.prev = block.prev;
    }
}