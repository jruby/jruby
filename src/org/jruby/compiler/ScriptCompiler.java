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
 * Copyright (C) 2006 Charles O Nutter <headius@headius.com>
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

package org.jruby.compiler;

import org.jruby.parser.StaticScope;

/**
 * Compiler represents the current state of a compiler and all appropriate
 * transitions and modifications that can be made within it. The methods here begin
 * and end a class for a given compile run, begin and end methods for the script being
 * compiled, set line number information, and generate code for all the basic
 * operations necessary for a script to run.
 * 
 * The intent of this interface is to provide a library-neutral set of functions for
 * compiling a given script using any backend or any output format.
 */
public interface ScriptCompiler {
    /**
     * Begin compilation for a script, preparing all necessary context and code
     * to support this script's compiled representation.
     */
    public void startScript(StaticScope scope);
    
    /**
     * End compilation for the current script, closing all context and structures
     * used for the compilation.
     */
    public void endScript(boolean generateLoad, boolean generateMain);
    
    /**
     * Begin compilation for a method that has the specified number of local variables.
     * The returned value is a token that can be used to end the method later.
     * 
     * @param javaName The outward user-readable name of the method. A unique name will be generated based on this.
     * @param arity The arity of the method's argument list
     * @param localVarCount The number of local variables that will be used by the method.
     * @return An Object that represents the method within this compiler. Used in calls to
     * endMethod once compilation for this method is completed.
     */
    public BodyCompiler startMethod(String rubyName, String javaName, CompilerCallback argsHandler, StaticScope scope, ASTInspector inspector);

    /**
     * Begin compilation for a the root of a script. This differs from method compilation
     * in that it doesn't do specific-arity logic, nor does it require argument processing.
     *
     * @param javaName The outward user-readable name of the method. A unique name will be generated based on this.
     * @param arity The arity of the method's argument list
     * @param localVarCount The number of local variables that will be used by the method.
     * @return An Object that represents the method within this compiler. Used in calls to
     * endMethod once compilation for this method is completed.
     */
    public BodyCompiler startRoot(String rubyName, String javaName, StaticScope scope, ASTInspector inspector);
    
    /**
     * Begin compilation for the root of a script named __file__.
     * 
     * @param args Arguments to the script, as passed via jitted wrappers
     * @param scope The StaticScope for the script
     * @param inspector The ASTInspector for the nodes for the script
     * @return A new BodyCompiler for the body of the script
     */
    public BodyCompiler startFileMethod(CompilerCallback args, StaticScope scope, ASTInspector inspector);
}
