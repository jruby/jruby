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
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004-2005 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
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

import org.jruby.Ruby;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.Node;
import org.jruby.ast.executable.Script;
import org.jruby.compiler.impl.StandardASMCompiler;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.DefaultMethod;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.util.JRubyClassLoader;
import org.jruby.util.JavaNameMangler;

public class JITCompiler {
    public static void runJIT(DefaultMethod method, Ruby runtime, ThreadContext context, String name) {
        if (method.getCallCount() >= 0) {
            try {
                method.setCallCount(method.getCallCount() + 1);

                if (method.getCallCount() >= runtime.getInstanceConfig().getJitThreshold()) {
                    String cleanName = JavaNameMangler.mangleStringForCleanJavaIdentifier(name);
                    String filename = "__eval__";
                    Node bodyNode = method.getBodyNode();
                    final ArgsNode argsNode = method.getArgsNode();
                    StaticScope staticScope = method.getStaticScope();
                    
                    if (bodyNode != null) {
                        filename = bodyNode.getPosition().getFile();
                    } else if (argsNode != null) {
                        filename = argsNode.getPosition().getFile();
                    }
                    StandardASMCompiler asmCompiler = new StandardASMCompiler(cleanName + method.hashCode() + "_" + context.hashCode(), filename);
                    asmCompiler.startScript(staticScope);
                    final ASTCompiler compiler = new ASTCompiler();
        
                    CompilerCallback args = new CompilerCallback() {
                        public void call(MethodCompiler context) {
                            compiler.compileArgs(argsNode, context);
                        }
                    };
        
                    ASTInspector inspector = new ASTInspector();
                    inspector.inspect(bodyNode);
                    inspector.inspect(argsNode);
                    
                    MethodCompiler methodCompiler;
                    CallConfiguration jitCallConfig = null;
                    if (bodyNode != null) {
                        // we have a body, do a full-on method
                        methodCompiler = asmCompiler.startMethod("__file__", args, staticScope, inspector);
                        compiler.compile(bodyNode, methodCompiler);
                    } else {
                        // If we don't have a body, check for required or opt args
                        // if opt args, they could have side effects
                        // if required args, need to raise errors if too few args passed
                        // otherwise, method does nothing, make it a nop
                        if (argsNode != null && (argsNode.getRequiredArgsCount() > 0 || argsNode.getOptionalArgsCount() > 0)) {
                            methodCompiler = asmCompiler.startMethod("__file__", args, staticScope, inspector);
                            methodCompiler.loadNil();
                        } else {
                            methodCompiler = asmCompiler.startMethod("__file__", null, staticScope, inspector);
                            methodCompiler.loadNil();
                            jitCallConfig = CallConfiguration.NO_FRAME_NO_SCOPE;
                        }
                    }
                    methodCompiler.endMethod();
                    asmCompiler.endScript();
                    Class sourceClass = asmCompiler.loadClass(new JRubyClassLoader(runtime.getJRubyClassLoader()));
                    
                    // if we haven't already decided on a do-nothing call
                    if (jitCallConfig == null) {
                        // if we're not doing any of the operations that still need a scope, use the scopeless config
                        if (!(inspector.hasClosure() || inspector.hasScopeAwareMethods())) {
                            // switch to a slightly faster call config
                            jitCallConfig = CallConfiguration.FRAME_ONLY;
                        } else {
                            jitCallConfig = CallConfiguration.FRAME_AND_SCOPE;
                        }
                    }
                    
                    // finally, grab the script
                    Script jitCompiledScript = (Script)sourceClass.newInstance();
                    
                    if (runtime.getInstanceConfig().isJitLogging()) {
                        String className = method.getImplementationClass().getBaseName();
                        if (className == null) {
                            className = "<anon class>";
                        }
                        System.err.println("compiled: " + className + "." + name);
                    }
                    method.setJITCallConfig(jitCallConfig);
                    method.setJITCompiledScript(jitCompiledScript);
                    method.setCallCount(-1);
                }
            } catch (Exception e) {
                if (runtime.getInstanceConfig().isJitLoggingVerbose()) {
                    String className = method.getImplementationClass().getBaseName();
                    if (className == null) {
                        className = "<anon class>";
                    }
                    System.err.println("could not compile: " + className + "." + name + " because of: \"" + e.getMessage() + '"');
                }
                method.setCallCount(-1);
             }
        }
    }
}
