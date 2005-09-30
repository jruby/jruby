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
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
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
package org.jruby.parser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.jruby.ast.BlockNode;
import org.jruby.ast.Node;
import org.jruby.lexer.yacc.ISourcePosition;

/**
 */
public class RubyParserResult {
    private final List beginNodes = new ArrayList();
    private final List endNodes = new ArrayList();
    private Node ast;
    private List blockVariables;
    private List localVariables;
    private InputStream afterEndStream;
    private boolean endSeen;

    /**
     * Constructor for RubyParserResult.
     */
    public RubyParserResult() {
        super();
    }

    /**
     * Gets the localVariables.
     * @return Returns a List
     */
    public List getLocalVariables() {
        return localVariables;
    }

    /**
     * Gets the blockVariables.
     * @return Returns a List
     */
    public List getBlockVariables() {
        return blockVariables;
    }

    /**
     * Gets the afterEndStream.
     * @return Returns a InputStream
     */
    public InputStream getAfterEndStream() {
        return afterEndStream;
    }

    public Node getAST() {
        return ast;
    }
    /**
     * Sets the afterEndStream.
     * @param afterEndStream The afterEndStream to set
     */
    public void setAfterEndStream(InputStream afterEndStream) {
        this.afterEndStream = afterEndStream;
    }

    /**
     * Sets the ast.
     * @param ast The ast to set
     */
    public void setAST(Node ast) {
        this.ast = ast;
    }

    /**
     * Sets the blockVariables.
     * @param blockVariables The blockVariables to set
     */
    public void setBlockVariables(List blockVariables) {
        this.blockVariables = blockVariables;
    }

    /**
     * Sets the localVariables.
     * @param localVariables The localVariables to set
     */
    public void setLocalVariables(List localVariables) {
        this.localVariables = localVariables;
    }
    
    public void addBeginNode(Node node) {
    	beginNodes.add(node);
    }
    
    public void addEndNode(Node node) {
    	endNodes.add(node);
    }
    
    public void addAppendBeginAndEndNodes() {
    	if (beginNodes.isEmpty() && endNodes.isEmpty()) {
    		return;
    	}
    	BlockNode n;
    	if (getAST() != null) {
    		n = new BlockNode(getAST().getPosition());
    	} else {
    		ISourcePosition p;
    		if (!beginNodes.isEmpty()) {
    			p = ((Node) beginNodes.get(0)).getPosition();
    		} else {
    			p = ((Node) endNodes.get(endNodes.size() - 1)).getPosition();
    		}
    		n = new BlockNode(p);
    	}
    	for (int i = 0; i < beginNodes.size(); i++) {
    		n.add((Node) beginNodes.get(i));
    	}
    	if (getAST() != null) {
    		n.add(getAST());
    	}
    	for (int i = endNodes.size() - 1; i >= 0; i--) {
    		n.add((Node) endNodes.get(i));
    	}
    	setAST(n);
    }
    
    public boolean isEndSeen() {
    	return endSeen;
    }
    
    public void setEndSeen(boolean endSeen) {
    	this.endSeen = endSeen;
    }
}
