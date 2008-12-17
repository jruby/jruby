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
 * Copyright (C) 2007 Ola Bini <ola.bini@gmail.com>
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
package org.jruby.ast.executable;

import org.jruby.MetaClass;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.parser.StaticScope;
import org.jruby.parser.LocalStaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.internal.runtime.methods.WrapperMethod;
import org.jruby.internal.runtime.methods.RubiniusMethod;
import org.jruby.javasupport.util.RuntimeHelpers;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class RubiniusMachine {
    public final static RubiniusMachine INSTANCE = new RubiniusMachine();

    public final static int getInt(char[] bytecodes, int ix) {
        int val = 0;
        val += (bytecodes[ix+0]<<24);
        val += (bytecodes[ix+1]<<16);
        val += (bytecodes[ix+2]<<8);
        val += (bytecodes[ix+3]);
        return val;
    }

    public IRubyObject exec(ThreadContext context, IRubyObject self, char[] bytecodes, IRubyObject[] literals, IRubyObject[] args) {
        IRubyObject[] stack = new IRubyObject[20];
        int stackTop = 0;
        stack[stackTop] = context.getRuntime().getNil();
        for(int i=0;i<args.length;i++) {
            stack[++stackTop] = args[i];
        }
        int ip = 0;
        int call_flags = -1;
        int cache_index = -1;
        Ruby runtime = context.getRuntime();
        IRubyObject recv;
        IRubyObject other;

        loop: while (ip < bytecodes.length) {
            int ix = ip;
            int code = bytecodes[ip++];
            /*
                System.err.print(RubiniusInstructions.NAMES[code] + " (" + code + ") "); 
                if(RubiniusInstructions.ONE_INT[code]) {
                    System.err.print("[" + getInt(bytecodes, ip) + "] ");
                } else if(RubiniusInstructions.TWO_INT[code]) {
                    System.err.print("[" + getInt(bytecodes, ip) + ", " + getInt(bytecodes, ip+4) + "] ");
                }
                System.err.println("{" + ix + "}");

                for(int i=stackTop; i>=0; i--) {
                    System.err.println(" [" + i + "]=" + stack[i].callMethod(context, "inspect"));
                    }*/
            switch(code) {
            case RubiniusInstructions.NOOP: {
                break;
            }
            case RubiniusInstructions.ADD_METHOD: {
                int val = getInt(bytecodes, ip);
                ip += 4;
                String name = literals[val].toString();
                RubyModule clzz = (RubyModule)stack[stackTop--];
                RubyArray method = (RubyArray)stack[stackTop--];
                
                Visibility visibility = context.getCurrentVisibility();
                if (name == "initialize" || visibility == Visibility.MODULE_FUNCTION) {
                    visibility = Visibility.PRIVATE;
                }
                
                RubiniusCMethod cmethod = new RubiniusCMethod(method);
                
                StaticScope staticScope = new LocalStaticScope(context.getCurrentScope().getStaticScope());
                staticScope.setVariables(new String[cmethod.locals]);
                staticScope.determineModule();

                RubiniusMethod newMethod = new RubiniusMethod(clzz, cmethod, staticScope, visibility);

                clzz.addMethod(name, newMethod);
    
                if (context.getCurrentVisibility() == Visibility.MODULE_FUNCTION) {
                    clzz.getSingletonClass().addMethod(
                            name,
                            new WrapperMethod(clzz.getSingletonClass(), newMethod,
                                    Visibility.PUBLIC));
                    clzz.callMethod(context, "singleton_method_added", literals[val]);
                }
    
                if (clzz.isSingleton()) {
                    ((MetaClass) clzz).getAttached().callMethod(
                            context, "singleton_method_added", literals[val]);
                } else {
                    clzz.callMethod(context, "method_added", literals[val]);
                }
                stack[++stackTop] = method;
                break;
            }
            case RubiniusInstructions.META_PUSH_NEG_1: {
                stack[++stackTop] = RubyFixnum.minus_one(runtime);
                break;
            }
            case RubiniusInstructions.CHECK_ARGCOUNT: {
                int min = getInt(bytecodes, ip);
                ip += 4;
                int max = getInt(bytecodes, ip);
                ip += 4;

                if(args.length < min) {
                    throw runtime.newArgumentError("wrong # of arguments(" + args.length + " for " + min + ")");
                } else if(max>0 && args.length>max) {
                    throw runtime.newArgumentError("wrong # of arguments(" + args.length + " for " + max + ")");
                }
                break;
            }
            case RubiniusInstructions.META_PUSH_0: {
                stack[++stackTop] = RubyFixnum.zero(runtime);
                break;
            }
            case RubiniusInstructions.META_PUSH_1: {
                stack[++stackTop] = RubyFixnum.one(runtime);
                break;
            }
            case RubiniusInstructions.META_PUSH_2: {
                stack[++stackTop] = runtime.newFixnum(2);
                break;
            }
            case RubiniusInstructions.SET_LOCAL: {
                int local = getInt(bytecodes, ip);
                ip += 4;
                context.getCurrentScope().setValue(local,stack[stackTop],0);
                break;
            }
            case RubiniusInstructions.PUSH_LOCAL: {
                int local = getInt(bytecodes, ip);
                ip += 4;
                stack[++stackTop] = context.getCurrentScope().getValue(local,0);
                break;
            }
            case RubiniusInstructions.PUSH_NIL: {
                stack[++stackTop] = runtime.getNil();
                break;
            }
            case RubiniusInstructions.PUSH_TRUE: {
                stack[++stackTop] = runtime.getTrue();
                break;
            }
            case RubiniusInstructions.PUSH_FALSE: {
                stack[++stackTop] = runtime.getFalse();
                break;
            }
            case RubiniusInstructions.PUSH_SELF: {
                stack[++stackTop] = self;
                break;
            }
            case RubiniusInstructions.STRING_DUP: {
                stack[stackTop] = ((RubyString)stack[stackTop]).strDup(context.getRuntime());
                break;
            }
            case RubiniusInstructions.PUSH_LITERAL: {
                int val = getInt(bytecodes, ip);
                ip += 4;
                stack[++stackTop] = literals[val];
                break;
            }
            case RubiniusInstructions.META_SEND_OP_LT: {
                IRubyObject t1 = stack[stackTop--];
                IRubyObject t2 = stack[stackTop--];
                if((t1 instanceof RubyFixnum) && (t2 instanceof RubyFixnum)) {
                    stack[++stackTop] = (((RubyFixnum)t1).getLongValue() < ((RubyFixnum)t2).getLongValue()) ? runtime.getTrue() : runtime.getFalse();
                } else {
                    stack[++stackTop] = t1.callMethod(context, "<", t2);
                }
                break;
            }

            case RubiniusInstructions.META_SEND_OP_GT: {
                IRubyObject t1 = stack[stackTop--];
                IRubyObject t2 = stack[stackTop--];
                if((t1 instanceof RubyFixnum) && (t2 instanceof RubyFixnum)) {
                    stack[++stackTop] = (((RubyFixnum)t1).getLongValue() > ((RubyFixnum)t1).getLongValue()) ? runtime.getTrue() : runtime.getFalse();
                } else {
                    stack[++stackTop] = t1.callMethod(context, ">", t2);
                }
                break;
            }

            case RubiniusInstructions.META_SEND_OP_PLUS: {
                IRubyObject t1 = stack[stackTop--];
                IRubyObject t2 = stack[stackTop--];
                if((t1 instanceof RubyFixnum) && (t2 instanceof RubyFixnum)) {
                    stack[++stackTop] = ((RubyFixnum)t1).op_plus(context, t2);
                } else {
                    stack[++stackTop] = t1.callMethod(context, "+", t2);
                }
                break;
            }
            case RubiniusInstructions.META_SEND_OP_MINUS: {

                IRubyObject t1 = stack[stackTop--];
                IRubyObject t2 = stack[stackTop--];
                if((t1 instanceof RubyFixnum) && (t2 instanceof RubyFixnum)) {
                    stack[++stackTop] = ((RubyFixnum)t1).op_minus(context, t2);
                } else {
                    stack[++stackTop] = t1.callMethod(context, "-", t2);
                }
                break;
            }
            case RubiniusInstructions.POP: {
                stackTop--;
                break;
            }
            case RubiniusInstructions.SET_CALL_FLAGS: {
                int val = getInt(bytecodes, ip);
                ip += 4;
                call_flags = val;
                break;
            }
            case RubiniusInstructions.SET_CACHE_INDEX: {
                int val = getInt(bytecodes, ip);
                ip += 4;
                cache_index = val;
                break;
            }
            case RubiniusInstructions.SEND_STACK: {
                int index = getInt(bytecodes, ip);
                ip += 4;
                int num_args = getInt(bytecodes, ip);
                ip += 4;
                
                String name = literals[index].toString();
                recv = stack[stackTop--];
                IRubyObject[] argu = new IRubyObject[num_args];
                for(int i=0;i<num_args;i++) {
                    argu[i] = stack[stackTop--];
                }
                if((call_flags & 0x01) == 0x01) { //Functional
                    stack[++stackTop] = RuntimeHelpers.invoke(context, recv, name, argu, Block.NULL_BLOCK); 
                } else {
                    stack[++stackTop] = RuntimeHelpers.invoke(context, recv, name, argu, CallType.NORMAL, Block.NULL_BLOCK); 
                }
                break;
            }
            case RubiniusInstructions.GOTO_IF_FALSE: {
                int val = getInt(bytecodes, ip);
                ip += 4;
                if(!stack[stackTop--].isTrue()) {
                    ip = val;
                }
                break;
            }
            case RubiniusInstructions.GOTO_IF_TRUE: {
                int val = getInt(bytecodes, ip);
                ip += 4;
                if(stack[stackTop--].isTrue()) {
                    ip = val;
                }
                break;
            }
            case RubiniusInstructions.SWAP_STACK: {
                IRubyObject swap = stack[stackTop];
                stack[stackTop] = stack[stackTop-1];
                stack[stackTop-1] = swap;
                break;
            }
            case RubiniusInstructions.DUP_TOP: {
                stack[stackTop+1] = stack[stackTop];
                stackTop++;
                break;
            }
            case RubiniusInstructions.GOTO: {
                int val = getInt(bytecodes, ip);
                ip += 4;
                ip = val;
                break;
            }
            case RubiniusInstructions.RET: {
                return stack[stackTop];
            }
            case RubiniusInstructions.PUSH_INT: {
                int val = getInt(bytecodes, ip);
                ip += 4;
                stack[++stackTop] = runtime.newFixnum(val);
                break;
            }
            case RubiniusInstructions.PUSH_CONST: {
                int index = getInt(bytecodes, ip);
                ip += 4;
                
                String name = literals[index].toString();
                stack[++stackTop] = context.getConstant(name);
                break;
            }
            default:
                System.err.println("--COULDN'T");
                if(RubiniusInstructions.ONE_INT[code]) {
                    ip+=4;
                } else if(RubiniusInstructions.TWO_INT[code]) {
                    ip+=8;
                }
                break;
            }
        }
        return null;
    }
}// RubiniusMachine
