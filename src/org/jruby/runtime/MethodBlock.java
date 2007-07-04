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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2004-2007 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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
package org.jruby.runtime;

import org.jruby.RubyArray;
import org.jruby.RubyMethod;
import org.jruby.RubyModule;
import org.jruby.RubyProc;
import org.jruby.exceptions.JumpException;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callback.Callback;
import org.jruby.util.collections.SinglyLinkedList;

/**
 *  Internal live representation of a block ({...} or do ... end).
 */
public class MethodBlock extends Block{

    /**
     * 'self' at point when the block is defined
     */
    private IRubyObject self;
    
    private RubyMethod method;
    private Callback callback;
    
    private final Arity arity;
    
    /**
     * frame of method which defined this block
     */
    protected final Frame frame;
    private final SinglyLinkedList cref;
    private Visibility visibility;
    private final RubyModule klass;
    
    /**
     * A reference to all variable values (and names) that are in-scope for this block.
     */
    private final DynamicScope dynamicScope;
    
    /**
     * The Proc that this block is associated with.  When we reference blocks via variable
     * reference they are converted to Proc objects.  We store a reference of the associated
     * Proc object for easy conversion.  
     */
    private RubyProc proc = null;
    
    public boolean isLambda = false;

    public static MethodBlock createMethodBlock(ThreadContext context, DynamicScope dynamicScope, Callback callback, RubyMethod method, IRubyObject self) {
        return new MethodBlock(self,
                               context.getCurrentFrame().duplicate(),
                         context.peekCRef(),
                         context.getCurrentFrame().getVisibility(),
                         context.getRubyClass(),
                         dynamicScope,
                         callback,
                         method);
    }

    public MethodBlock(IRubyObject self, Frame frame,
        SinglyLinkedList cref, Visibility visibility, RubyModule klass,
        DynamicScope dynamicScope, Callback callback, RubyMethod method) {

        this.self = self;
        this.frame = frame;
        this.visibility = visibility;
        this.klass = klass;
        this.cref = cref;
        this.dynamicScope = dynamicScope;
        this.callback = callback;
        this.method = method;
        this.arity = Arity.createArity((int) method.arity().getLongValue());
    }

    public IRubyObject call(ThreadContext context, IRubyObject[] args) {
        return yield(context, args, null, null, true);
    }
    
    protected void pre(ThreadContext context, RubyModule klass) {
        context.preYieldSpecificBlock(this, klass);
    }
    
    protected void post(ThreadContext context) {
        context.postYield();
    }
    
    public IRubyObject yield(ThreadContext context, IRubyObject value) {
        return yield(context, new IRubyObject[] {value}, null, null, false);
    }

    /**
     * Yield to this block, usually passed to the current call.
     * 
     * @param context represents the current thread-specific data
     * @param value The value to yield, either a single value or an array of values
     * @param self The current self
     * @param klass
     * @param aValue Should value be arrayified or not?
     * @return
     */
    public IRubyObject yield(ThreadContext context, IRubyObject[] args, IRubyObject self, 
            RubyModule klass, boolean aValue) {
        if (klass == null) {
            self = this.self;
            frame.setSelf(self);
        }
        
        pre(context, klass);

        try {
            // This while loop is for restarting the block call in case a 'redo' fires.
            while (true) {
                try {
                    return callback.execute(RubyArray.newArrayNoCopyLight(context.getRuntime(), args), new IRubyObject[] { method, self }, NULL_BLOCK);
                } catch (JumpException je) {
                    if (je.getJumpType() == JumpException.JumpType.RedoJump) {
                        context.pollThreadEvents();
                        // do nothing, allow loop to redo
                    } else {
                        if (je.getJumpType() == JumpException.JumpType.BreakJump && je.getTarget() == null) {
                            je.setTarget(this);                            
                        }                        
                        throw je;
                    }
                }
            }
            
        } catch (JumpException je) {
            // A 'next' is like a local return from the block, ending this call or yield.
        	if (je.getJumpType() == JumpException.JumpType.NextJump) return (IRubyObject) je.getValue();

            throw je;
        } finally {
            post(context);
        }
    }

    public Block cloneBlock() {
        // We clone dynamic scope because this will be a new instance of a block.  Any previously
        // captured instances of this block may still be around and we do not want to start
        // overwriting those values when we create a new one.
        // ENEBO: Once we make self, lastClass, and lastMethod immutable we can remove duplicate
        Block newBlock = new MethodBlock(self, frame.duplicate(), cref, visibility, klass, 
                dynamicScope.cloneScope(), callback, method);
        
        newBlock.isLambda = isLambda;

        return newBlock;
    }

    /**
     * What is the arity of this block?
     * 
     * @return the arity
     */
    public Arity arity() {
        return arity;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }
    
    public void setSelf(IRubyObject self) {
        this.self = self;
    }

    public SinglyLinkedList getCRef() {
        return cref;
    }

    /**
     * Retrieve the proc object associated with this block
     * 
     * @return the proc or null if this has no proc associated with it
     */
    public RubyProc getProcObject() {
    	return proc;
    }
    
    /**
     * Set the proc object associated with this block
     * 
     * @param procObject
     */
    public void setProcObject(RubyProc procObject) {
    	this.proc = procObject;
    }

    /**
     * Gets the dynamicVariables that are local to this block.   Parent dynamic scopes are also
     * accessible via the current dynamic scope.
     * 
     * @return Returns all relevent variable scoping information
     */
    public DynamicScope getDynamicScope() {
        return dynamicScope;
    }

    /**
     * Gets the frame.
     * 
     * @return Returns a RubyFrame
     */
    public Frame getFrame() {
        return frame;
    }

    /**
     * Gets the klass.
     * @return Returns a RubyModule
     */
    public RubyModule getKlass() {
        return klass;
    }
    
    /**
     * Is the current block a real yield'able block instead a null one
     * 
     * @return true if this is a valid block or false otherwise
     */
    public boolean isGiven() {
        return true;
    }
}
