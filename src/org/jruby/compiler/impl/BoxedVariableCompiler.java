/*
 ***** BEGIN LICENSE BLOCK *****
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

package org.jruby.compiler.impl;

import org.jruby.compiler.CompilerCallback;
import org.jruby.parser.StaticScope;

/**
 *
 * @author headius
 */
public class BoxedVariableCompiler extends HeapBasedVariableCompiler {
    private StaticScope scope; // the static scope for the currently compiling method

    public BoxedVariableCompiler(
            StandardASMCompiler.AbstractMethodCompiler methodCompiler,
            SkinnyMethodAdapter method,
            int scopeIndex,
            int varsIndex,
            int argsIndex,
            int closureIndex) {
        super(methodCompiler, method,scopeIndex, varsIndex, argsIndex, closureIndex);
        
        this.scopeIndex = scopeIndex;
        this.varsIndex = varsIndex;
    }

    public void beginMethod(CompilerCallback argsCallback, StaticScope scope) {
        this.scope = scope; 
        
        // fill non-captured Java local vars with nil as well
        if (scope != null) {
            methodCompiler.loadNil();
            for (int i = 0; i < scope.getNumberOfVariables(); i++) {
                if (scope.isCaptured(i)) continue;
                
                assignLocalVariable(i);
            }
            method.pop();
        }
        
        super.beginMethod(argsCallback, scope);
    }

    public void beginClass(CompilerCallback bodyPrep, StaticScope scope) {
        assert false : "Do not use boxed var compiler for class bodies";
    }

    public void beginClosure(CompilerCallback argsCallback, StaticScope scope) {
        this.scope = scope;

        // fill non-captured Java local vars with nil as well
        if (scope != null) {
            methodCompiler.loadNil();
            for (int i = 0; i < scope.getNumberOfVariables(); i++) {
                if (scope.isCaptured(i)) continue;
                
                assignLocalVariable(i);
            }
            method.pop();
        }
        
        super.beginClosure(argsCallback, scope);
    }

    public void assignLocalVariable(int index) {
        if (scope.isCaptured(index)) {
            super.assignLocalVariable(index);
        } else {
            // non-captured var, just use locals
            method.dup();
            method.astore(10 + index);
        }
    }

    public void retrieveLocalVariable(int index) {
        if (scope.isCaptured(index)) {
            super.retrieveLocalVariable(index);
        } else {
            // non-captured, use java local vars
            method.aload(10 + index);
        }
    }
}
