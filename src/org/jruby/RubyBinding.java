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
import org.jruby.runtime.DynamicVariableSet;
import org.jruby.runtime.Frame;
import org.jruby.runtime.Iter;
import org.jruby.runtime.ThreadContext;

/**
 * @author  jpetersen
 */
public class RubyBinding extends RubyObject {
    private Block block = null;
    private RubyModule wrapper = null;

    public RubyBinding(IRuby runtime, RubyClass rubyClass, Block block, RubyModule wrapper) {
        super(runtime, rubyClass);
        
        this.block = block;
        this.wrapper = wrapper;
    }

    public Block getBlock() {
        return block;
    }

    public RubyModule getWrapper() {
        return wrapper;
    }

    // Proc class
    
    public static RubyBinding newBinding(IRuby runtime, Block block) {
        return new RubyBinding(runtime, runtime.getClass("Binding"), block, block.getKlass());
    }

    public static RubyBinding newBinding(IRuby runtime) {
        ThreadContext context = runtime.getCurrentContext();
        
        // FIXME: We should be cloning, not reusing: frame, scope, dynvars, and potentially iter/block info
        RubyModule wrapper = context.getWrapper();
        Iter iter = context.getCurrentIter();
        Frame frame = context.getPreviousFrame();
        // for TOPLEVEL_BINDING
        if (frame == null) {
            frame = context.getCurrentFrame();
        }
        DynamicVariableSet dynVars = context.getCurrentDynamicVars();
        Block bindingBlock = Block.createBinding(wrapper, iter, frame, dynVars);

        RubyBinding newBinding = new RubyBinding(runtime, runtime.getClass("Binding"), bindingBlock, context.getRubyClass());

        return newBinding;
    }
}
