/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import java.math.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.dsl.*;
import org.jruby.truffle.nodes.cast.BoxingNode;
import org.jruby.truffle.nodes.dispatch.Dispatch;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.UseMethodMissingException;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;

@CoreClass(name = "Math")
public abstract class MathNodes {

    @CoreMethod(names = "acos", isModuleMethod = true, needsSelf = false, minArgs = 1, maxArgs = 1)
    public abstract static class ACosNode extends CoreMethodNode {

        public ACosNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ACosNode(ACosNode prev) {
            super(prev);
        }

        @Specialization
        public double acos(int a) {
            return Math.acos(a);
        }

        @Specialization
        public double acos(BigInteger a) {
            return Math.acos(a.doubleValue());
        }

        @Specialization
        public double acos(double a) {
            return Math.acos(a);
        }

    }

    @CoreMethod(names = "acosh", isModuleMethod = true, needsSelf = false, minArgs = 1, maxArgs = 1)
    public abstract static class ACosHNode extends CoreMethodNode {

        public ACosHNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ACosHNode(ACosHNode prev) {
            super(prev);
        }

        @Specialization
        public double acos(int a) {
            return acosh(a);
        }

        @Specialization
        public double acos(BigInteger a) {
            return acosh(a.doubleValue());
        }

        @Specialization
        public double acos(double a) {
            return acosh(a);
        }

        private static double acosh(double a) {
            return Math.log(a + Math.sqrt(a * a - 1));
        }

    }

    @CoreMethod(names = "asin", isModuleMethod = true, needsSelf = false, minArgs = 1, maxArgs = 1)
    public abstract static class ASinNode extends CoreMethodNode {

        public ASinNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ASinNode(ASinNode prev) {
            super(prev);
        }

        @Specialization
        public double asin(int a) {
            return Math.asin(a);
        }

        @Specialization
        public double asin(BigInteger a) {
            return Math.asin(a.doubleValue());
        }

        @Specialization
        public double asin(double a) {
            return Math.asin(a);
        }

    }

    @CoreMethod(names = "exp", isModuleMethod = true, needsSelf = false, minArgs = 1, maxArgs = 1)
    public abstract static class ExpNode extends CoreMethodNode {

        public ExpNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ExpNode(ExpNode prev) {
            super(prev);
        }

        @Specialization
        public double exp(int a) {
            return Math.exp(a);
        }

        @Specialization
        public double exp(BigInteger a) {
            return Math.exp(a.doubleValue());
        }

        @Specialization
        public double exp(double a) {
            return Math.exp(a);
        }

    }

    @CoreMethod(names = "log", isModuleMethod = true, needsSelf = false, minArgs = 1, maxArgs = 1)
    public abstract static class LogNode extends CoreMethodNode {

        public LogNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public LogNode(LogNode prev) {
            super(prev);
        }

        @Specialization
        public double log(int a) {
            return Math.log(a);
        }

        @Specialization
        public double log(BigInteger a) {
            return Math.log(a.doubleValue());
        }

        @Specialization
        public double log(double a) {
            return Math.log(a);
        }

    }

    @CoreMethod(names = "log10", isModuleMethod = true, needsSelf = false, minArgs = 1, maxArgs = 1)
    public abstract static class Log10Node extends CoreMethodNode {

        public Log10Node(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public Log10Node(Log10Node prev) {
            super(prev);
        }

        @Specialization
        public double log10(int a) {
            return Math.log10(a);
        }

        @Specialization
        public double log10(BigInteger a) {
            return Math.log10(a.doubleValue());
        }

        @Specialization
        public double log10(double a) {
            return Math.log10(a);
        }

    }

    @CoreMethod(names = "sin", isModuleMethod = true, needsSelf = false, minArgs = 1, maxArgs = 1)
    public abstract static class SinNode extends CoreMethodNode {

        @Child protected BoxingNode box;
        @Child protected DispatchHeadNode floatNode;

        public SinNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            box = new BoxingNode(context, sourceSection);
            floatNode = new DispatchHeadNode(context, Dispatch.MissingBehavior.RETURN_MISSING);
        }

        public SinNode(SinNode prev) {
            super(prev);
            box = prev.box;
            floatNode = prev.floatNode;
        }

        @Specialization
        public double sin(int a) {
            return Math.sin(a);
        }

        @Specialization
        public double sin(long a) {
            return Math.sin(a);
        }

        @Specialization
        public double sin(BigInteger a) {
            return Math.sin(a.doubleValue());
        }

        @Specialization
        public double sin(double a) {
            return Math.sin(a);
        }

        @Fallback
        public double sin(VirtualFrame frame, Object a) {
            final RubyBasicObject boxed = box.box(a);

            if (boxed.isNumeric()) {
                try {
                    return Math.sin(floatNode.callFloat(frame, box.box(a), "to_f", null));
                } catch (UseMethodMissingException e) {
                    throw new RaiseException(getContext().getCoreLibrary().typeErrorCantConvertInto(
                            box.box(a).getRubyClass().getName(),
                            getContext().getCoreLibrary().getFloatClass().getName(),
                            this));
                }
            } else {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(getContext().getCoreLibrary().typeErrorCantConvertInto(
                        box.box(a).getRubyClass().getName(),
                        getContext().getCoreLibrary().getFloatClass().getName(),
                        this));
            }
        }

    }

    @CoreMethod(names = "sqrt", isModuleMethod = true, needsSelf = false, minArgs = 1, maxArgs = 1)
    public abstract static class SqrtNode extends CoreMethodNode {

        public SqrtNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SqrtNode(SqrtNode prev) {
            super(prev);
        }

        @Specialization
        public double sqrt(int a) {
            return Math.sqrt(a);
        }

        @Specialization
        public double sqrt(BigInteger a) {
            return Math.sqrt(a.doubleValue());
        }

        @Specialization
        public double sqrt(double a) {
            return Math.sqrt(a);
        }

    }

}
