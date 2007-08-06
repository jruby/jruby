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
 * Copyright (C) 2006 Mirko Stocker <me@misto.ch>
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

package org.jruby.ast.visitor.rewriter;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;

import junit.framework.TestCase;

import org.jruby.ast.ArgumentNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.ConstNode;
import org.jruby.ast.LocalVarNode;
import org.jruby.ast.Node;
import org.jruby.ast.OptNNode;
import org.jruby.ast.PostExeNode;
import org.jruby.ast.RegexpNode;
import org.jruby.ast.visitor.rewriter.ReWriteVisitor;
import org.jruby.lexer.yacc.IDESourcePosition;
import org.jruby.util.ByteList;

public class TestReWriteVisitor extends TestCase {

    static final IDESourcePosition emptyPosition = new IDESourcePosition("", 0, 0, 0, 0);

    private String visitNode(Node n) {
        StringWriter out = new StringWriter();
        ReWriteVisitor visitor = new ReWriteVisitor(out, "");
        n.accept(visitor);
        visitor.flushStream();
        return out.getBuffer().toString();
    }

    public void testVisitRegexpNode() {
        RegexpNode n = new RegexpNode(new IDESourcePosition("", 0, 0, 2, 4), ByteList.create(".*"), 0);
        assertEquals("/.*/", visitNode(n));
    }

    public void testGetLocalVarIndex() {
        assertEquals(ReWriteVisitor.getLocalVarIndex(new LocalVarNode(emptyPosition, 5, "")), 5);
        assertEquals(ReWriteVisitor.getLocalVarIndex(new LocalVarNode(emptyPosition, 1, "")), 1);
        assertEquals(ReWriteVisitor.getLocalVarIndex(null), -1);
    }

    private ReWriteVisitor getVisitor() {
        return new ReWriteVisitor(new StringWriter(), "");
    }

    public void testVisitOptNNode() {
        assertNull(getVisitor().visitOptNNode(new OptNNode(emptyPosition, null)));
    }

    public void testVisitPostExeNode() {
        assertNull(getVisitor().visitPostExeNode(new PostExeNode(emptyPosition, null)));
    }

    public void testUnwrapSingleArrayNode() {
        ArrayNode arrayNode = new ArrayNode(emptyPosition);
        ConstNode constNode = new ConstNode(emptyPosition, "const");
        ConstNode anotherConstNode = new ConstNode(emptyPosition, "const");
        arrayNode.add(constNode);

        assertEquals(ReWriteVisitor.unwrapSingleArrayNode(arrayNode), constNode);
        assertEquals(ReWriteVisitor.unwrapSingleArrayNode(constNode), constNode);

        arrayNode.add(anotherConstNode);
        assertEquals(ReWriteVisitor.unwrapSingleArrayNode(arrayNode), arrayNode);
    }

    public void testUnescapeChar() {
        assertEquals(ReWriteVisitor.unescapeChar('\f'), "f");
        assertEquals(ReWriteVisitor.unescapeChar('\r'), "r");
        assertEquals(ReWriteVisitor.unescapeChar('\t'), "t");
        assertEquals(ReWriteVisitor.unescapeChar('n'), null);
    }

    public void testArgumentNode() {
        Node node = new ArgumentNode(new IDESourcePosition(), "name");
        assertEquals("name", ReWriteVisitor.createCodeFromNode(node, ""));
    }

    public void testFileOutputStream() throws IOException {
        String fileName = "outputTest";
        try {
            String testString = "test";
            FileOutputStream stream = new FileOutputStream(fileName);
            ReWriteVisitor visitor = new ReWriteVisitor(stream, "");
            ConstNode node = new ConstNode(emptyPosition, testString);
            node.accept(visitor);
            visitor.flushStream();
            stream.close();
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            assertEquals(reader.readLine(), testString);
        } finally {
            new java.io.File(fileName).delete();
        }
    }
}



