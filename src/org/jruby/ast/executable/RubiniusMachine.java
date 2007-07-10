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
import org.jruby.parser.StaticScope;
import org.jruby.parser.LocalStaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class RubiniusMachine {
    public final static RubiniusMachine INSTANCE = new RubiniusMachine();

    public IRubyObject exec(ThreadContext context, IRubyObject self, StaticScope scope, char[] bytecodes) {
        return exec(context,self, new DynamicScope(scope),bytecodes);
    }

    public final static int getInt(char[] bytecodes, int ix) {
        int val = 0;
        val += (bytecodes[ix+0]<<24);
        val += (bytecodes[ix+1]<<16);
        val += (bytecodes[ix+2]<<8);
        val += (bytecodes[ix+3]);
        return val;
    }

    public IRubyObject exec(ThreadContext context, IRubyObject self, DynamicScope scope, char[] bytecodes) {
        IRubyObject[] stack = new IRubyObject[255];
        int stackTop = 0;
        stack[stackTop] = context.getRuntime().getNil();
        int ip = 0;
        Ruby runtime = context.getRuntime();
        context.preRootNode(scope);
        IRubyObject recv;
        IRubyObject other;

        loop: while (ip < bytecodes.length) {
            int ix = ip;
            int code = bytecodes[ip++];
            System.err.print(RubiniusInstructions.NAMES[code] + " (" + code + ") "); 
            if(RubiniusInstructions.ONE_INT[code]) {
                System.err.print("[" + getInt(bytecodes, ip) + "] ");
                ip+=4;
            } else if(RubiniusInstructions.TWO_INT[code]) {
                System.err.print("[" + getInt(bytecodes, ip) + ", " + getInt(bytecodes, ip+4) + "] ");
                ip+=8;
            }
            System.err.println("{" + ix + "}");
        }
        return null;
    }
}// RubiniusMachine
