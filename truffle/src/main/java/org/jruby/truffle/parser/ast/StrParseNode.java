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
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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
package org.jruby.truffle.parser.ast;

import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.core.string.StringSupport;
import org.jruby.truffle.parser.ParserByteList;
import org.jruby.truffle.parser.ParserByteListBuilder;
import org.jruby.truffle.parser.TempSourceSection;
import org.jruby.truffle.parser.ast.types.ILiteralNode;
import org.jruby.truffle.parser.ast.visitor.NodeVisitor;

import java.util.List;

/** 
 * Representing a simple String literal.
 */
public class StrParseNode extends ParseNode implements ILiteralNode, SideEffectFree {
    private ParserByteList value;
    private final CodeRange codeRange;
    private boolean frozen;

    public StrParseNode(TempSourceSection position, ParserByteList value) {
        this(position, value, StringSupport.codeRangeScan(value.getEncoding(), value));
    }

    public StrParseNode(TempSourceSection position, ParserByteList value, CodeRange codeRange) {
        super(position, false);

        this.value = value;
        this.codeRange = codeRange;
    }

    public StrParseNode(TempSourceSection position, StrParseNode head, StrParseNode tail) {
        super(position, false);

        ParserByteList headBL = head.getValue();
        ParserByteList tailBL = tail.getValue();

        ParserByteListBuilder myValue = new ParserByteListBuilder();
        myValue.setEncoding(headBL.getEncoding());
        myValue.append(headBL);
        myValue.append(tailBL);

        frozen = head.isFrozen() && tail.isFrozen();
        value = myValue.toParserByteList();
        codeRange = StringSupport.codeRangeScan(value.getEncoding(), value);
    }

    public NodeType getNodeType() {
        return NodeType.STRNODE;
    }
    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public <T> T accept(NodeVisitor<T> iVisitor) {
        return iVisitor.visitStrNode(this);
    }

    /**
     * Gets the value.
     * @return Returns a String
     */
    public ParserByteList getValue() {
        return value;
    }

    /**
     * Get the string's coderange.
     *
     * @return the string's coderange
     */
    public CodeRange getCodeRange() {
        return codeRange;
    }
    
    public List<ParseNode> childNodes() {
        return EMPTY_LIST;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    public void setValue(ParserByteList value) {
        this.value = value;
    }
}
