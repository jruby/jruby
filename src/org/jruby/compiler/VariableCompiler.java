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


package org.jruby.compiler;

import org.jruby.parser.StaticScope;

import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.objectweb.asm.Label;

/**
 *
 * @author headius
 */
public interface VariableCompiler {
    public SkinnyMethodAdapter getMethodAdapter();
    public void setMethodAdapter(SkinnyMethodAdapter sma);
    public void beginMethod(CompilerCallback argsCallback, StaticScope scope);
    public void beginClass(StaticScope scope);
    public void beginClosure(CompilerCallback argsCallback, StaticScope scope);
    public void beginFlatClosure(CompilerCallback argsCallback, StaticScope scope);
    public void assignLocalVariable(int index, boolean expr);
    public void assignLocalVariable(int index, int depth, boolean expr);
    public void assignLocalVariable(int index, int depth, CompilerCallback value, boolean expr);
    public void retrieveLocalVariable(int index);
    public void assignLastLine();
    public void assignLastLine(CompilerCallback value);
    public void retrieveLastLine();
    public void assignBackRef();
    public void assignBackRef(CompilerCallback value);
    public void retrieveBackRef();
    public void retrieveLocalVariable(int index, int depth);
    public void checkMethodArity(int requiredArgs, int optArgs, int restArg);
    public void assignMethodArguments(
            Object requiredArgs,
            int requiredArgsCount,
            Object optArgs,
            int optArgsCount,
            ArrayCallback requiredAssignment,
            ArrayCallback optGivenAssignment,
            ArrayCallback optNotGivenAssignment,
            CompilerCallback restAssignment,
            CompilerCallback blockAssignment);
    public void assignMethodArguments19(
            Object preArgs,
            int preArgsCount,
            Object postArgs,
            int postArgsCount,
            int postArgsIndex,
            Object optArgs,
            int optArgsCount,
            ArrayCallback requiredAssignment,
            ArrayCallback optGivenAssignment,
            ArrayCallback optNotGivenAssignment,
            CompilerCallback restAssignment,
            CompilerCallback blockAssignment);
    public void assignClosureArguments(
            CompilerCallback masgnCallback,
            CompilerCallback blockAssignment);
    public int grabTempLocal();
    public void setTempLocal(int index);
    public void getTempLocal(int index);
    public void releaseTempLocal();
    public void declareLocals(StaticScope scope, Label start, Label end);
    public boolean isHeap();
}
