/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.rope;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.core.string.StringUtils;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.platform.UnsafeGroup;

@CoreClass("Truffle::Ropes")
public abstract class TruffleRopesNodes {

    @CoreMethod(names = "debug_print", onSingleton = true, required = 1, unsafe = UnsafeGroup.IO)
    public abstract static class DebugPrintNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = "isRubyString(string)")
        public DynamicObject debugPrint(DynamicObject string) {
            System.err.println(StringOperations.getString(string));
            return nil();
        }

    }

    @CoreMethod(names = "dump_string", onSingleton = true, required = 1)
    public abstract static class DumpStringNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = "isRubyString(string)")
        public DynamicObject dumpString(DynamicObject string) {
            final StringBuilder builder = new StringBuilder();

            final Rope rope = StringOperations.rope(string);

            for (int i = 0; i < rope.byteLength(); i++) {
                builder.append(StringUtils.format("\\x%02x", rope.get(i)));
            }

            return createString(StringOperations.encodeRope(builder.toString(), UTF8Encoding.INSTANCE));
        }

    }

    @CoreMethod(names = "convert_to_mutable_rope", onSingleton = true, required = 1)
    public abstract static class ConvertToMutableRope extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isRubyString(string)")
        public DynamicObject convertToMutableRope(DynamicObject string) {
            final RopeBuffer ropeBuffer = new RopeBuffer(StringOperations.rope(string));
            StringOperations.setRope(string, ropeBuffer);

            return string;
        }
    }

    @CoreMethod(names = "debug_print_rope", onSingleton = true, required = 1, optional = 1, unsafe = UnsafeGroup.IO)
    public abstract static class DebugPrintRopeNode extends CoreMethodArrayArgumentsNode {

        @Child private RopeNodes.DebugPrintRopeNode debugPrintRopeNode = RopeNodesFactory.DebugPrintRopeNodeGen.create(null, null, null);

        @TruffleBoundary
        @Specialization(guards = "isRubyString(string)")
        public DynamicObject debugPrintDefault(DynamicObject string, NotProvided printString) {
            return debugPrint(string, true);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(string)")
        public DynamicObject debugPrint(DynamicObject string, boolean printString) {
            System.err.println("Legend: ");
            System.err.println("BN = Bytes Null? (byte[] not yet populated)");
            System.err.println("BL = Byte Length");
            System.err.println("CL = Character Length");
            System.err.println("CR = Code Range");
            System.err.println("O = Offset (SubstringRope only)");
            System.err.println("T = Times (RepeatingRope only)");
            System.err.println("D = Depth");
            System.err.println("LD = Left Depth (ConcatRope only)");
            System.err.println("RD = Right Depth (ConcatRope only)");

            return debugPrintRopeNode.executeDebugPrint(StringOperations.rope(string), 0, printString);
        }
    }

    /**
     * The returned string (when evaluated) will create a string with the same
     * Rope structure as the string which is passed as argument.
     */
    @CoreMethod(names = "debug_get_structure_creation", onSingleton = true, required = 1)
    public abstract static class DebugGetStructureCreationNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = "isRubyString(string)")
        public DynamicObject getStructure(DynamicObject string) {
            Rope rope = StringOperations.rope(string);
            String result = getStructure(rope);
            return createString(RopeOperations.create(result.getBytes(), rope.getEncoding(), CodeRange.CR_7BIT));
        }

        protected static String getStructure(Rope rope) {
            if (rope instanceof LeafRope) {
                return getStructure((LeafRope) rope);
            } else if (rope instanceof ConcatRope) {
                return getStructure((ConcatRope) rope);
            } else if (rope instanceof SubstringRope) {
                return getStructure((SubstringRope) rope);
            } else if (rope instanceof RepeatingRope) {
                return getStructure((RepeatingRope) rope);
            } else {
                return "(unknown rope class: " + rope.getClass() + ")";
            }
        }

        private static String getStructure(LeafRope rope) {
            return "\"" + rope.toString() + "\"";
        }

        private static String getStructure(ConcatRope rope) {
            return "(" + getStructure(rope.getLeft()) + " + " + getStructure(rope.getRight()) + ")";
        }

        private static String getStructure(SubstringRope rope) {
            return getStructure(rope.getChild()) + "[" + rope.getOffset() + ", " + rope.characterLength() + "]";
        }

        private static String getStructure(RepeatingRope rope) {
            return "(" + getStructure(rope.getChild()) + "*" + rope.getTimes() + ")";
        }

    }

    @CoreMethod(names = "flatten_rope", onSingleton = true, required = 1)
    public abstract static class FlattenRopeNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isRubyString(string)")
        public DynamicObject flattenRope(DynamicObject string,
                                         @Cached("create()") RopeNodes.FlattenNode flattenNode) {
            final Rope flattened = flattenNode.executeFlatten(StringOperations.rope(string));

            return createString(flattened);
        }

    }

    /*
     * Truffle.create_simple_string creates a string 'test' without any part of the string escaping. Useful
     * for testing compilation of String becuase most other ways to construct a string can currently escape.
     */

    @CoreMethod(names = "create_simple_string", onSingleton = true)
    public abstract static class CreateSimpleStringNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject createSimpleString() {
            return createString(new AsciiOnlyLeafRope(new byte[]{'t', 'e', 's', 't'}, UTF8Encoding.INSTANCE));
        }
    }

}
