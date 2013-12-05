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
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2006 Lukas Felber <lfelber@hsr.ch>
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
import org.jruby.RubyClass;
import org.jruby.RubyString;
import org.jruby.ast.types.IArityNode;
import org.jruby.ast.types.INameNode;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.ivars.VariableAccessor;
import org.jruby.util.DefinedMessage;

/** 
 * Represents an instance variable accessor.
 */
public class InstVarNode extends Node implements IArityNode, INameNode {
    private String name;
    private VariableAccessor accessor = VariableAccessor.DUMMY_ACCESSOR;

    public InstVarNode(ISourcePosition position, String name) {
        super(position);
        this.name = name;
    }

    public NodeType getNodeType() {
        return NodeType.INSTVARNODE;
    }
    
    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public Object accept(NodeVisitor iVisitor) {
        return iVisitor.visitInstVarNode(this);
    }

	/**
	 * A variable accessor takes no arguments.
	 */
	public Arity getArity() {
		return Arity.noArguments();
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
    
    public void setName(String name){
        this.name = name;
    }
    
    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        return getVariable(runtime, self, true);
    }

    private IRubyObject getVariable(Ruby runtime, IRubyObject self, boolean warn) {
        IRubyObject value = getValue(runtime, self);
        if (value != null) return value;
        if (warn && runtime.isVerbose()) warnAboutUninitializedIvar(runtime);
        return runtime.getNil();
    }

    private IRubyObject getValue(Ruby runtime, IRubyObject self) {
        RubyClass cls = self.getMetaClass().getRealClass();
        VariableAccessor localAccessor = accessor;
        IRubyObject value;
        if (localAccessor.getClassId() != cls.hashCode()) {
            localAccessor = cls.getVariableAccessorForRead(name);
            if (localAccessor == null) return runtime.getNil();
            value = (IRubyObject)localAccessor.get(self);
            accessor = localAccessor;
        } else {
            value = (IRubyObject)localAccessor.get(self);
        }
        return value;
    }

    private void warnAboutUninitializedIvar(Ruby runtime) {
        runtime.getWarnings().warning(ID.IVAR_NOT_INITIALIZED, getPosition(), 
                "instance variable " + name + " not initialized");
    }
    
    @Override
    public RubyString definition(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        return getValue(runtime, self) == null ? null : runtime.getDefinedMessage(DefinedMessage.INSTANCE_VARIABLE);
    }
}
