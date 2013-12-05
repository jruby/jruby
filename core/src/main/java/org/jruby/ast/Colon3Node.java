/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004-2006 Thomas E Enebo <enebo@acm.org>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ast;

import java.util.List;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.runtime.opto.ConstantCache;
import org.jruby.ast.executable.RuntimeCache;
import org.jruby.ast.types.INameNode;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.exceptions.JumpException;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.opto.Invalidator;
import org.jruby.util.ByteList;
import org.jruby.util.DefinedMessage;

/**
 * Global scope node (::FooBar).  This is used to gain access to the global scope (that of the 
 * Object class) when referring to a constant or method.
 */
public class Colon3Node extends Node implements INameNode {
    protected String name;
    protected ConstantCache cache;
    
    public Colon3Node(ISourcePosition position, String name) {
        super(position);
        this.name = name;
    }

    public NodeType getNodeType() {
        return NodeType.COLON3NODE;
    }
    
    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public Object accept(NodeVisitor iVisitor) {
        return iVisitor.visitColon3Node(this);
    }

    /**
     * Gets the name.
     * @return Returns a String
     */
    public String getName() {
        return name;
    }
    
    public List<Node> childNodes() {
        return EMPTY_LIST;
    }

    public void setName(String name) {
        this.name = name;
        this.cache = null;
    }
    
   /** Get parent module/class that this module represents */
    public RubyModule getEnclosingModule(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        return runtime.getObject();
    }
    
    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        IRubyObject value = getValue(context);

        // We can callsite cache const_missing if we want
        return value != null ? value : runtime.getObject().getConstantFromConstMissing(name);
    }
    
    @Override
    public RubyString definition(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        try {
            RubyModule left = runtime.getObject();

            return Helpers.getDefinedConstantOrBoundMethod(left, name);
        } catch (JumpException excptn) {
        }
            
        return null;
    }

    public IRubyObject getValue(ThreadContext context) {
        ConstantCache cache = this.cache;

        return ConstantCache.isCached(cache) ? cache.value : reCache(context, name);
    }

    public IRubyObject reCache(ThreadContext context, String name) {
        Ruby runtime = context.runtime;
        Invalidator invalidator = runtime.getConstantInvalidator(name);
        Object newGeneration = invalidator.getData();
        IRubyObject value = runtime.getObject().getConstantFromNoConstMissing(name, false);

        if (value != null) {
            cache = new ConstantCache(value, newGeneration, invalidator);
        } else {
            cache = null;
        }

        return value;
    }
}
