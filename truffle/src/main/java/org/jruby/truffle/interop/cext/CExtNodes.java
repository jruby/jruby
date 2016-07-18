/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.interop.cext;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.CoreMethodNode;
import org.jruby.truffle.core.cast.NameToJavaStringNodeGen;
import org.jruby.truffle.language.RubyConstant;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.constants.GetConstantNode;
import org.jruby.truffle.language.constants.LookupConstantNode;

@CoreClass("Truffle::CExt")
public class CExtNodes {

    @CoreMethod(names = "NUM2INT", isModuleFunction = true, required = 1)
    public abstract static class NUM2INTNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int num2int(int num) {
            return num;
        }

    }

    @CoreMethod(names = "NUM2UINT", isModuleFunction = true, required = 1)
    public abstract static class NUM2UINTNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int num2uint(int num) {
            // TODO CS 2-May-16 what to do about the fact it's unsigned?
            return num;
        }

    }

    @CoreMethod(names = "NUM2LONG", isModuleFunction = true, required = 1)
    public abstract static class NUM2LONGNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public long num2long(int num) {
            return num;
        }

    }

    @CoreMethod(names = "FIX2INT", isModuleFunction = true, required = 1)
    public abstract static class FIX2INTNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int fix2int(int num) {
            return num;
        }

    }

    @CoreMethod(names = "FIX2UINT", isModuleFunction = true, required = 1)
    public abstract static class FIX2UINTNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int fix2uint(int num) {
            // TODO CS 2-May-16 what to do about the fact it's unsigned?
            return num;
        }

        @Specialization
        public long fix2uint(long num) {
            // TODO CS 2-May-16 what to do about the fact it's unsigned?
            return num;
        }

    }

    @CoreMethod(names = "FIX2LONG", isModuleFunction = true, required = 1)
    public abstract static class FIX2LONGNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public long fix2long(int num) {
            return num;
        }

    }

    @CoreMethod(names = "INT2NUM", isModuleFunction = true, required = 1)
    public abstract static class INT2NUMNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int int2num(int num) {
            return num;
        }

        @Specialization
        public long int2num(long num) {
            return num;
        }

    }

    @CoreMethod(names = "INT2FIX", isModuleFunction = true, required = 1)
    public abstract static class INT2FIXNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int int2fix(int num) {
            return num;
        }

        @Specialization
        public long int2fix(long num) {
            return num;
        }

    }

    @CoreMethod(names = "UINT2NUM", isModuleFunction = true, required = 1)
    public abstract static class UINT2NUMNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int uint2num(int num) {
            // TODO CS 2-May-16 what to do about the fact it's unsigned?
            return num;
        }

    }

    @CoreMethod(names = "LONG2NUM", isModuleFunction = true, required = 1)
    public abstract static class LONG2NUMNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int long2num(int num) {
            return num;
        }

    }

    @CoreMethod(names = "LONG2FIX", isModuleFunction = true, required = 1)
    public abstract static class LONG2FIXNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int long2fix(int num) {
            return num;
        }

    }

    @CoreMethod(names = "CExtString", isModuleFunction = true, required = 1)
    public abstract static class CExtStringNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isRubyString(string)")
        public CExtString cExtString(DynamicObject string) {
            return new CExtString(string);
        }

    }

    @CoreMethod(names = "to_ruby_string", isModuleFunction = true, required = 1)
    public abstract static class ToRubyStringNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject toRubyString(CExtString cExtString) {
            return cExtString.getString();
        }

        @Specialization(guards = "isRubyString(string)")
        public DynamicObject toRubyString(DynamicObject string) {
            return string;
        }

    }

    @CoreMethod(names = "get_block", isModuleFunction = true)
    public abstract static class GetBlockNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject getBlock() {
            return Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<DynamicObject>() {
                @Override
                public DynamicObject visitFrame(FrameInstance frameInstance) {
                    Frame frame = frameInstance.getFrame(FrameAccess.READ_ONLY, true);
                    return RubyArguments.tryGetBlock(frame);
                }
            });
        }

    }

    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "module"),
            @NodeChild(type = RubyNode.class, value = "name")
    })
    @CoreMethod(names = "rb_const_get_from", isModuleFunction = true, required = 2)
    public abstract static class ConstGetFromNode extends CoreMethodNode {

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return NameToJavaStringNodeGen.create(name);
        }

        @Child LookupConstantNode lookupConstantNode = LookupConstantNode.create(true, false);
        @Child GetConstantNode getConstantNode = GetConstantNode.create();

        @Specialization
        public Object constGetFrom(VirtualFrame frame, DynamicObject module, String name) {
            final RubyConstant constant = lookupConstantNode.lookupConstant(frame, module, name);
            return getConstantNode.executeGetConstant(frame, module, name, constant, lookupConstantNode);
        }

    }

}
