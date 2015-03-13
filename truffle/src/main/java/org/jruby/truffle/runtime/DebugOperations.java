/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.object.BooleanLocation;
import com.oracle.truffle.api.object.FinalLocationException;
import com.oracle.truffle.api.object.IncompatibleLocationException;
import com.oracle.truffle.api.object.Location;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubySymbol;
import org.jruby.truffle.runtime.methods.InternalMethod;

import java.util.ArrayList;
import java.util.List;

public abstract class DebugOperations {

    public static String inspect(RubyContext context, Object object) {
        CompilerAsserts.neverPartOfCompilation();

        final Object inspected = send(context, object, "inspect", null);

        if (inspected == null) {
            return String.format("%s@%x", object.getClass().getSimpleName(), object.hashCode());
        }

        return inspected.toString();
    }

    public static Object send(RubyContext context, Object object, String methodName, RubyProc block, Object... arguments) {
        CompilerAsserts.neverPartOfCompilation();

        final InternalMethod method = ModuleOperations.lookupMethod(context.getCoreLibrary().getMetaClass(object), methodName);

        if (method == null) {
            return null;
        }

        return method.getCallTarget().call(
                RubyArguments.pack(method, method.getDeclarationFrame(), object, block, arguments));
    }

    public static void panic(RubyContext context, Node currentNode, String message) {
        CompilerDirectives.transferToInterpreter();

        System.err.println("=========================== JRuby+Truffle Debug Report ========================");

        if (message != null) {
            System.err.println();
            System.err.println("Stopped because: " + message);
        }

        System.err.println();
        System.err.println("    =========================== Ruby Bracktrace ===========================    ");
        System.err.println();

        try {
            for (String line : Backtrace.PANIC_FORMATTER.format(context, null, RubyCallStack.getBacktrace(currentNode))) {
                System.err.println(line);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        System.err.println();
        System.err.println("    ========================== AST Backtrace ==========================    ");
        System.err.println();

        try {
            printASTBacktrace(currentNode);
        } catch (Throwable e) {
            e.printStackTrace();
        }

        System.err.println();
        System.err.println("    =========================== Java Backtrace ============================    ");
        System.err.println();

        new Exception().printStackTrace();

        System.err.println();
        System.err.println("===============================================================================");

        System.exit(1);
    }

    public static void printBacktrace(RubyContext context, Node currentNode) {
        for (String line : Backtrace.DISPLAY_FORMATTER.format(context, null, RubyCallStack.getBacktrace(currentNode))) {
            System.err.println(line);
        }
    }

    public static void printASTBacktrace(final Node currentNode) {
        if (currentNode != null) {
            printMethodASTBacktrace(currentNode);
        }

        Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<Object>() {

            @Override
            public Object visitFrame(FrameInstance frameInstance) {
                printMethodASTBacktrace(frameInstance.getCallNode());
                return null;
            }

        });
    }

    private static void printMethodASTBacktrace(Node currentNode) {
        final List<Node> activeNodes = new ArrayList<>();
        activeNodes.addAll(NodeUtil.findAllParents(currentNode, Node.class));
        activeNodes.add(currentNode);
        printASTForBacktrace(currentNode.getRootNode(), activeNodes, 0);
    }

    public static Object verySlowFreeze(Object o) {
        if ((o instanceof Boolean) ||
                (o instanceof Integer) ||
                (o instanceof Long) ||
                (o instanceof Double) ||
                (o instanceof RubySymbol)) {
            return o;
        }

        final RubyBasicObject object = (RubyBasicObject) o;

        object.getOperations().setInstanceVariable(object, RubyBasicObject.FROZEN_IDENTIFIER, true);

        return o;
    }

    public static boolean verySlowIsFrozen(Object o) {
        if ((o instanceof Boolean) ||
                (o instanceof Integer) ||
                (o instanceof Long) ||
                (o instanceof Double) ||
                (o instanceof RubySymbol)) {
            return true;
        }

        final RubyBasicObject object = (RubyBasicObject) o;

        final Shape layout = object.getDynamicObject().getShape();
        final Property property = layout.getProperty(RubyBasicObject.FROZEN_IDENTIFIER);

        if (property == null) {
            return false;
        }

        final Location storageLocation = property.getLocation();

        return (boolean) storageLocation.get(object.getDynamicObject(), layout);
    }

    public static boolean verySlowIsTainted(Object o) {
        if ((o instanceof Boolean) ||
                (o instanceof Integer) ||
                (o instanceof Long) ||
                (o instanceof Double) ||
                (o instanceof RubySymbol)) {
            return false;
        }

        final RubyBasicObject object = (RubyBasicObject) o;

        final Shape layout = object.getDynamicObject().getShape();
        final Property property = layout.getProperty(RubyBasicObject.TAINTED_IDENTIFIER);

        final Location storageLocation = property.getLocation();

        return (boolean) storageLocation.get(object.getDynamicObject(), layout);
    }

    private static void printASTForBacktrace(Node node, List<Node> activeNodes, int indentation) {
        for (int n = 0; n < indentation; n++) {
            System.err.print("  ");
        }

        if (activeNodes.contains(node)) {
            System.err.print("-> ");
        } else {
            System.err.print("   ");
        }

        System.err.println(node);

        for (Node child : node.getChildren()) {
            if (child != null) {
                printASTForBacktrace(child, activeNodes, indentation + 1);
            }
        }
    }

}
