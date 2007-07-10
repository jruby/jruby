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

import org.jruby.Ruby;
import org.jruby.RubyFixnum;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.parser.StaticScope;
import org.jruby.parser.LocalStaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class RubiniusMachine {
    public final static RubiniusMachine INSTANCE = new RubiniusMachine();

    public IRubyObject exec(ThreadContext context, IRubyObject self, StaticScope scope, char[] bytecodes, IRubyObject[] literals) {
        return exec(context,self, new DynamicScope(scope),bytecodes,literals);
    }

    public final static int getInt(char[] bytecodes, int ix) {
        int val = 0;
        val += (bytecodes[ix+0]<<24);
        val += (bytecodes[ix+1]<<16);
        val += (bytecodes[ix+2]<<8);
        val += (bytecodes[ix+3]);
        return val;
    }

    public IRubyObject exec(ThreadContext context, IRubyObject self, DynamicScope scope, char[] bytecodes, IRubyObject[] literals) {
        IRubyObject[] stack = new IRubyObject[20];
        int stackTop = 0;
        stack[stackTop] = context.getRuntime().getNil();
        int ip = 0;
        int call_flags = -1;
        int cache_index = -1;
        Ruby runtime = context.getRuntime();
        context.preRootNode(scope);
        IRubyObject recv;
        IRubyObject other;

        loop: while (ip < bytecodes.length) {
            int ix = ip;
            int code = bytecodes[ip++];
            switch(code) {
            case RubiniusInstructions.NOOP: {
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
                stack[stackTop] = ((RubyString)stack[stackTop]).strDup();
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
                if((t1 instanceof RubyFixnum) && (t1 instanceof RubyFixnum)) {
                    stack[++stackTop] = (RubyNumeric.fix2int(t1) < RubyNumeric.fix2int(t2)) ? runtime.getTrue() : runtime.getFalse();
                } else {
                    stack[++stackTop] = t1.callMethod(runtime.getCurrentContext(), MethodIndex.OP_LT, "<", t2);
                }
                break;
            }
            case RubiniusInstructions.META_SEND_OP_PLUS: {
                IRubyObject t1 = stack[stackTop--];
                IRubyObject t2 = stack[stackTop--];
                if((t1 instanceof RubyFixnum) && (t1 instanceof RubyFixnum)) {
                    stack[++stackTop] = runtime.newFixnum(RubyNumeric.fix2int(t1) + RubyNumeric.fix2int(t2));
                } else {
                    stack[++stackTop] = t1.callMethod(runtime.getCurrentContext(), MethodIndex.OP_PLUS, "+", t2);
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
                int ixi = MethodIndex.getIndex(name);
                recv = stack[stackTop--];
                if((call_flags & 0x01) == 0x01) { //Functional
                    IRubyObject[] args = new IRubyObject[num_args];
                    for(int i=0;i<num_args;i++) {
                        args[i] = stack[stackTop--];
                    }
                    stack[++stackTop] = recv.callMethod(runtime.getCurrentContext(), recv.getMetaClass(), ixi, name, args, CallType.FUNCTIONAL, Block.NULL_BLOCK); 
                } else {
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
            default:
                System.err.print(RubiniusInstructions.NAMES[code] + " (" + code + ") "); 
                if(RubiniusInstructions.ONE_INT[code]) {
                    System.err.print("[" + getInt(bytecodes, ip) + "] ");
                    ip+=4;
                } else if(RubiniusInstructions.TWO_INT[code]) {
                    System.err.print("[" + getInt(bytecodes, ip) + ", " + getInt(bytecodes, ip+4) + "] ");
                    ip+=8;
                }
                System.err.println("{" + ix + "}");
                break;
            }
        }
        return null;
    }
}// RubiniusMachine
