/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.interop;

import com.oracle.truffle.api.dsl.Specialization;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;

@CoreClass("Truffle::CExt")
public class CExtNodes {

    @CoreMethod(names = "NUM2INT", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class NUM2INTNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int num2int(int num) {
            return num;
        }

    }

    @CoreMethod(names = "NUM2UINT", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class NUM2UINTNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int num2uint(int num) {
            // TODO CS 2-May-16 what to do about the fact it's unsigned?
            return num;
        }

    }

    @CoreMethod(names = "NUM2LONG", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class NUM2LONGNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public long num2long(int num) {
            return num;
        }

    }

    @CoreMethod(names = "FIX2INT", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class FIX2INTNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int fix2int(int num) {
            return num;
        }

    }

    @CoreMethod(names = "FIX2UINT", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class FIX2UINTNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int fix2uint(int num) {
            // TODO CS 2-May-16 what to do about the fact it's unsigned?
            return num;
        }

    }

    @CoreMethod(names = "FIX2LONG", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class FIX2LONGNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public long fix2long(int num) {
            return num;
        }

    }

    @CoreMethod(names = "INT2NUM", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class INT2NUMNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int int2num(int num) {
            return num;
        }

    }

    @CoreMethod(names = "INT2FIX", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class INT2FIXNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int int2fix(int num) {
            return num;
        }

    }

    @CoreMethod(names = "UINT2NUM", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class UINT2NUMNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int uint2num(int num) {
            // TODO CS 2-May-16 what to do about the fact it's unsigned?
            return num;
        }

    }

    @CoreMethod(names = "LONG2NUM", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class LONG2NUMNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int long2num(int num) {
            return num;
        }

    }

    @CoreMethod(names = "LONG2FIX", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class LONG2FIXNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int long2fix(int num) {
            return num;
        }

    }

}
