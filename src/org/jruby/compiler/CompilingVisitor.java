/*
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with JRuby; if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
 */
package org.jruby.compiler;

import org.jruby.ast.visitor.AbstractVisitor;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.FixnumNode;
import org.jruby.util.Asserts;
import org.jruby.compiler.bytecodes.AssignLocal;
import org.jruby.compiler.bytecodes.PushFixnum;
import org.ablaf.ast.INode;

public class CompilingVisitor extends AbstractVisitor {
    private ByteCodeSequence byteCode = new ByteCodeSequence();

    public ByteCodeSequence compile(INode tree) {
        emitByteCode(tree);
        return byteCode;
    }

    private void emitByteCode(INode node) {
        node.accept(this);
    }

    public void visitNewlineNode(NewlineNode node) {
        emitByteCode(node.getNextNode());
    }

    public void visitLocalAsgnNode(LocalAsgnNode node) {
        emitByteCode(node.getValueNode());
        byteCode.add(new AssignLocal(node.getCount()));
    }

    public void visitFixnumNode(FixnumNode node) {
        byteCode.add(new PushFixnum(node.getValue()));
    }

    public void visitNode(INode node) {
        Asserts.notReached("unsupported node: " + node.getClass());
    }
}
