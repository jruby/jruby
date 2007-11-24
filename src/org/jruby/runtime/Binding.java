/*
 ***** BEGIN LICENSE BLOCK *****
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

import org.jruby.RubyModule;
import org.jruby.parser.BlockStaticScope;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *  Internal live representation of a block ({...} or do ... end).
 */
public class Binding {
    /**
     * 'self' at point when the block is defined
     */
    private IRubyObject self;
    
    /**
     * frame of method which defined this block
     */
    private final Frame frame;
    private Visibility visibility;
    private RubyModule klass;
    
    /**
     * A reference to all variable values (and names) that are in-scope for this block.
     */
    private final DynamicScope dynamicScope;
    
    public Binding(IRubyObject self, Frame frame,
            Visibility visibility, RubyModule klass, DynamicScope dynamicScope) {
        this.self = self;
        this.frame = frame;
        this.visibility = visibility;
        this.klass = klass;
        this.dynamicScope = dynamicScope;
    }
    
    public Binding(Frame frame, RubyModule bindingClass, DynamicScope dynamicScope) {
        this.self = frame.getSelf();
        this.frame = frame;
        this.visibility = frame.getVisibility();
        this.klass = bindingClass;
        this.dynamicScope = dynamicScope;
    }

    public Binding cloneBinding() {
        // We clone dynamic scope because this will be a new instance of a block.  Any previously
        // captured instances of this block may still be around and we do not want to start
        // overwriting those values when we create a new one.
        // ENEBO: Once we make self, lastClass, and lastMethod immutable we can remove duplicate
        Binding newBlock = new Binding(self, frame.duplicate(), visibility, klass, 
                dynamicScope.cloneScope());

        return newBlock;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }
    
    public IRubyObject getSelf() {
        return self;
    }
    
    public void setSelf(IRubyObject self) {
        this.self = self;
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
