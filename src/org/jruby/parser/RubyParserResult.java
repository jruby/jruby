/*
 * RubyParserResult.java - description
 * Created on 04.03.2002, 12:47:27
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
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
package org.jruby.parser;

import org.ablaf.ast.INode;

import java.io.InputStream;
import java.util.List;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RubyParserResult implements IRubyParserResult {
    private INode beginNodes;
    private INode ast;
    private List blockVariables;
    private List localVariables;
    private InputStream afterEndStream;

    /**
     * Constructor for RubyParserResult.
     */
    public RubyParserResult() {
        super();
    }

    /**
     * Gets the beginNodes.
     * @return Returns a INode
     */
    public INode getBeginNodes() {
        return beginNodes;
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

    /**
     * @see IParserResult#getAST()
     */
    public INode getAST() {
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
    public void setAST(INode ast) {
        this.ast = ast;
    }

    /**
     * Sets the beginNodes.
     * @param beginNodes The beginNodes to set
     */
    public void setBeginNodes(INode beginNodes) {
        this.beginNodes = beginNodes;
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
}