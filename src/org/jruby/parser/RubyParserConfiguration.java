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
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
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

import java.util.List;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RubyParserConfiguration {
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
