/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.ext.truffelize;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.MethodNodes;
import org.jruby.internal.runtime.methods.MethodWithNodes;
import org.jruby.internal.runtime.methods.UndefinedMethod;
import org.jruby.runtime.load.Library;
import org.jruby.truffle.TruffleMethod;

public class TruffelizeLibrary implements Library {

    @Override
    public void load(Ruby runtime, boolean wrap) {
        RubyClass moduleClass = runtime.getModule();
        moduleClass.defineAnnotatedMethods(TruffelizeModule.class);

        RubyModule kernelModule = runtime.getKernel();
        kernelModule.defineAnnotatedMethods(TruffelizeKernel.class);
    }

    public static void truffelize(RubyModule module, String name) {
        final DynamicMethod method = module.searchMethod(name);

        if (method instanceof UndefinedMethod) {
            throw module.getRuntime().newRuntimeError("method not found");
        }

        if (method instanceof TruffleMethod) {
            throw module.getRuntime().newRuntimeError("already truffelized");
        }

        if (!(method instanceof MethodWithNodes)) {
            throw module.getRuntime().newRuntimeError("can only truffelize methods where JRuby can provide the nodes for us");
        }

        final MethodWithNodes methodWithNodes = (MethodWithNodes) method;
        final MethodNodes methodNodes = methodWithNodes.getMethodNodes();

        if (methodNodes == null || methodNodes.getArgsNode() == null || methodNodes.getBodyNode() == null) {
            throw module.getRuntime().newRuntimeError("can only truffelize methods where JRuby can provide the nodes for us");
        }

        final TruffleMethod truffleMethod = module.getRuntime().getTruffleBridge().truffelize(method, methodNodes.getArgsNode(), methodNodes.getBodyNode());

        module.addMethod(name, truffleMethod);
    }

}
