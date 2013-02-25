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

import org.jruby.runtime.CallType;

import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.internal.runtime.methods.DynamicMethod.NativeCall;

/**
 *
 * @author headius
 */
public interface InvocationCompiler {
    public SkinnyMethodAdapter getMethodAdapter();
    public void setMethodAdapter(SkinnyMethodAdapter sma);
    
    /**
     * Invoke the named method as a "function", i.e. as a method on the current "self"
     * object, using the specified argument count. It is expected that previous calls
     * to the compiler has prepared the exact number of argument values necessary for this
     * call. Those values will be consumed, and the result of the call will be generated.
     */
    public void invokeDynamic(String name, CompilerCallback receiverCallback, ArgumentsCallback argsCallback, CallType callType, CompilerCallback closureArg, boolean iterator);
    
    /**
     * Same as invokeDynamic, but uses incoming IRubyObject[] arg count to dispatch
     * to the proper-arity path.
     */
    public void invokeDynamicVarargs(String name, CompilerCallback receiverCallback, ArgumentsCallback argsCallback, CallType callType, CompilerCallback closureArg, boolean iterator);
    
    public void invokeOpAsgnWithOr(String attrName, String attrAsgnName, CompilerCallback receiverCallback, ArgumentsCallback argsCallback);
    public void invokeOpAsgnWithAnd(String attrName, String attrAsgnName, CompilerCallback receiverCallback, ArgumentsCallback argsCallback);
    public void invokeOpAsgnWithMethod(String opName, String attrName, String attrAsgnName, CompilerCallback receiverCallback, ArgumentsCallback argsCallback);
    
    /**
     * Attr assign calls have slightly different semantics that normal calls, so this method handles those additional semantics.
     */
    /**
     * The masgn version has the value to be assigned already on the stack,
     * and so uses a different path to perform the assignment.
     * 
     * @param name
     * @param receiverCallback
     * @param argsCallback
     */
    public void invokeAttrAssignMasgn(String name, CompilerCallback receiverCallback, ArgumentsCallback argsCallback, boolean selfCall);
    public void invokeAttrAssign(String name, CompilerCallback receiverCallback, ArgumentsCallback argsCallback, boolean selfCall, boolean expr);
    
    public void opElementAsgnWithOr(CompilerCallback receiverCallback, ArgumentsCallback argsCallback, CompilerCallback valueCallback);
    
    public void opElementAsgnWithAnd(CompilerCallback receiverCallback, ArgumentsCallback argsCallback, CompilerCallback valueCallback);
    
    public void opElementAsgnWithMethod(CompilerCallback receiverCallback, ArgumentsCallback argsCallback, CompilerCallback valueCallback, String operator);
    
    /**
     * Invoke the block passed into this method, or throw an error if no block is present.
     * If arguments have been prepared for the block, specify true. Otherwise the default
     * empty args will be used.
     */
    public void yield(CompilerCallback argsCallback, boolean unwrap);

    /**
     * Invoke the block passed into this method, or throw an error if no block is present.
     * If arguments have been prepared for the block, specify true. Otherwise the default
     * empty args will be used.
     */
    public void yield19(CompilerCallback argsCallback, boolean unsplat);

    /**
     * Invoke the block passed into this method, or throw an error if no block is present.
     * If arguments have been prepared for the block, specify true. Otherwise the default
     * empty args will be used. Use specific-arity call paths if possible.
     */
    public void yieldSpecific(ArgumentsCallback argsCallback);
    
    /**
     * Used for when nodes with a case; assumes stack is ..., case_value, when_cond_array
     */
    public void invokeEqq(ArgumentsCallback receivers, CompilerCallback argument);

    public void invokeBinaryFixnumRHS(String name, CompilerCallback receiverCallback, long fixnum);
    public void invokeBinaryBooleanFixnumRHS(String name, CompilerCallback receiverCallback, long fixnum);
    public void invokeBinaryFloatRHS(String name, CompilerCallback receiverCallback, double flote);
}
