/*
 * RubyParserConfiguration.java - description
 * Created on 04.03.2002, 13:20:52
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

import java.util.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RubyParserConfiguration implements IRubyParserConfiguration {
    private boolean classNest;
    private boolean compileForEval;
    private List blockVariables;
    private List localVariables;

    /**
     * Constructor for RubyParserConfiguration.
     */
    public RubyParserConfiguration() {
        super();
    }
    
    /**
     * Gets the blockVariables.
     * @return Returns a List
     */
    public List getBlockVariables() {
        return blockVariables;
    }

    /**
     * Sets the blockVariables.
     * @param blockVariables The blockVariables to set
     */
    public void setBlockVariables(List blockVariables) {
        this.blockVariables = blockVariables;
    }

    /**
     * Gets the classNest.
     * @return Returns a boolean
     */
    public boolean isClassNest() {
        return classNest;
    }

    /**
     * Sets the classNest.
     * @param classNest The classNest to set
     */
    public void setClassNest(boolean classNest) {
        this.classNest = classNest;
    }

    /**
     * Gets the compileForEval.
     * @return Returns a boolean
     */
    public boolean isCompileForEval() {
        return compileForEval;
    }

    /**
     * Sets the compileForEval.
     * @param compileForEval The compileForEval to set
     */
    public void setCompileForEval(boolean compileForEval) {
        this.compileForEval = compileForEval;
    }

    /**
     * Gets the localVariables.
     * @return Returns a List
     */
    public List getLocalVariables() {
        return localVariables;
    }

    /**
     * Sets the localVariables.
     * @param localVariables The localVariables to set
     */
    public void setLocalVariables(List localVariables) {
        this.localVariables = localVariables;
    }
}
