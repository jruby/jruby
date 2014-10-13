package org.jruby.truffle.nodes.rubinius;


import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.core.CoreClass;
import org.jruby.truffle.nodes.core.CoreMethod;
import org.jruby.truffle.nodes.core.CoreMethodNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyObject;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.rubinius.RubiniusByteArray;
import org.jruby.truffle.runtime.util.TypeConversionUtils;

@CoreClass(name = "ByteArray")
public abstract class ByteArrayNodes {
    @CoreMethod(names = "allocate", onSingleton = true)
    public abstract static class AllocateNode extends CoreMethodNode {
        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AllocateNode(AllocateNode prev) {
            super(prev);
        }

        @Specialization
        public RubyObject allocate(RubyObject baClass, RubyObject size) {
            throw new RaiseException(getContext().getCoreLibrary().typeError("ByteArray cannot be created via allocate()", this));
        }
    }

    @CoreMethod(names = {"new", "allocate_sized"}, onSingleton = true, minArgs = 1, maxArgs = 1)
    public abstract static class AllocateSizedNode extends CoreMethodNode {
        @Child
        protected DispatchHeadNode bytesToIntNode;

        public AllocateSizedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            bytesToIntNode = new DispatchHeadNode(context);
        }

        public AllocateSizedNode(AllocateSizedNode prev) {
            super(prev);
            bytesToIntNode = prev.bytesToIntNode;
        }

        @Specialization
        public RubyObject allocate_sized(VirtualFrame frame, RubyObject bytes) {
            RubyClass self = (RubyClass) RubyArguments.getSelf(frame.getArguments());
            return RubiniusByteArray.allocate_sized(this, self, TypeConversionUtils.convertToLong(this, bytesToIntNode, frame, bytes));
        }
    }

    @CoreMethod(names = "fetch_bytes", minArgs = 2, maxArgs = 2)
    public abstract static class FetchBytesNode extends CoreMethodNode {
        @Child
        protected DispatchHeadNode countToIntNode;
        @Child
        protected DispatchHeadNode startToIntNode;

        public FetchBytesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            startToIntNode = new DispatchHeadNode(context);
            countToIntNode = new DispatchHeadNode(context);
        }

        public FetchBytesNode(FetchBytesNode prev) {
            super(prev);
            startToIntNode = prev.startToIntNode;
            countToIntNode = prev.countToIntNode;
        }

        @Specialization
        public RubyObject fetch_bytes(VirtualFrame frame, RubiniusByteArray self, RubyObject start, RubyObject count) {
            return self.fetch_bytes(this, TypeConversionUtils.convertToLong(this, startToIntNode, frame, start), TypeConversionUtils.convertToLong(this, countToIntNode, frame, count));
        }
    }

    @CoreMethod(names = "move_bytes")
    public abstract static class MoveBytesNode extends CoreMethodNode {
        @Child
        protected DispatchHeadNode destToIntNode;
        @Child
        protected DispatchHeadNode countToIntNode;
        @Child
        protected DispatchHeadNode startToIntNode;

        public MoveBytesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            startToIntNode = new DispatchHeadNode(context);
            countToIntNode = new DispatchHeadNode(context);
            destToIntNode = new DispatchHeadNode(context);
        }

        public MoveBytesNode(MoveBytesNode prev) {
            super(prev);
            startToIntNode = prev.startToIntNode;
            countToIntNode = prev.countToIntNode;
            destToIntNode = prev.destToIntNode;
        }

        @Specialization
        public RubyObject move_bytes(VirtualFrame frame, RubiniusByteArray self, RubyObject start, RubyObject count, RubyObject dest) {
            self.move_bytes(this, TypeConversionUtils.convertToLong(this, startToIntNode, frame, start), TypeConversionUtils.convertToLong(this, countToIntNode, frame, count),
                    TypeConversionUtils.convertToLong(this, destToIntNode, frame, dest));
            return count;
        }
    }

    @CoreMethod(names = {"get_byte", "[]"})
    public abstract static class GetByteNode extends CoreMethodNode {
        @Child
        protected DispatchHeadNode indexToIntNode;

        public GetByteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            indexToIntNode = new DispatchHeadNode(context);
        }

        public GetByteNode(GetByteNode prev) {
            super(prev);
            indexToIntNode = prev.indexToIntNode;
        }

        @Specialization
        public int get_byte(VirtualFrame frame, RubiniusByteArray self, RubyObject index) {
            return self.get_byte(this, TypeConversionUtils.convertToLong(this, indexToIntNode, frame, index));
        }
    }

    @CoreMethod(names = {"set_byte", "[]="})
    public abstract static class SetByteNode extends CoreMethodNode {
        @Child
        protected DispatchHeadNode valueToIntNode;
        @Child
        protected DispatchHeadNode indexToIntNode;

        public SetByteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            indexToIntNode = new DispatchHeadNode(context);
            valueToIntNode = new DispatchHeadNode(context);
        }

        public SetByteNode(SetByteNode prev) {
            super(prev);
            indexToIntNode = prev.indexToIntNode;
            valueToIntNode = prev.valueToIntNode;
        }

        @Specialization
        public int set_byte(VirtualFrame frame, RubiniusByteArray self, RubyObject index, RubyObject value) {
            return self.set_byte(this, TypeConversionUtils.convertToLong(this, indexToIntNode, frame, index), TypeConversionUtils.convertToLong(this, valueToIntNode, frame, value));
        }
    }

    @CoreMethod(names = "compare_bytes")
    public abstract static class CompareBytesNode extends CoreMethodNode {
        @Child
        protected DispatchHeadNode bToIntNode;
        @Child
        protected DispatchHeadNode aToIntNode;

        public CompareBytesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            aToIntNode = new DispatchHeadNode(context);
            bToIntNode = new DispatchHeadNode(context);
        }

        public CompareBytesNode(CompareBytesNode prev) {
            super(prev);
            aToIntNode = prev.aToIntNode;
            bToIntNode = prev.bToIntNode;
        }

        @Specialization
        public int compare_bytes(VirtualFrame frame, RubiniusByteArray self, RubiniusByteArray other, RubyObject a, RubyObject b) {
            return self.compare_bytes(this, other, TypeConversionUtils.convertToLong(this, aToIntNode, frame, a), TypeConversionUtils.convertToLong(this, bToIntNode, frame, b));
        }
    }

    @CoreMethod(names = "size")
    public abstract static class SizeNode extends CoreMethodNode {
        public SizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SizeNode(SizeNode prev) {
            super(prev);
        }

        @Specialization
        public int size(RubiniusByteArray self) {
            return self.size();
        }
    }

    @CoreMethod(names = "locate")
    public abstract static class LocateNode extends CoreMethodNode {
        @Child
        protected DispatchHeadNode max_oToIntNode;
        @Child
        protected DispatchHeadNode startToIntNode;
        @Child
        protected DispatchHeadNode patternToStringNode;

        public LocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            patternToStringNode = new DispatchHeadNode(context);
            startToIntNode = new DispatchHeadNode(context);
            max_oToIntNode = new DispatchHeadNode(context);
        }

        public LocateNode(LocateNode prev) {
            super(prev);
            patternToStringNode = prev.patternToStringNode;
            startToIntNode = prev.startToIntNode;
            max_oToIntNode = prev.max_oToIntNode;
        }

        @Specialization
        public Object locate(VirtualFrame frame, RubiniusByteArray self, RubyObject pattern, RubyObject start, RubyObject max_o) {
            return self.locate((RubyString) patternToStringNode.call(frame, pattern, "to_s", null), TypeConversionUtils.convertToLong(this, startToIntNode, frame, start),
                    TypeConversionUtils.convertToLong(this, max_oToIntNode, frame, max_o));
        }
    }

    @CoreMethod(names = "prepend")
    public abstract static class PrependNode extends CoreMethodNode {
        @Child
        protected DispatchHeadNode strToStringNode;

        public PrependNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            strToStringNode = new DispatchHeadNode(context);
        }

        public PrependNode(PrependNode prev) {
            super(prev);
            strToStringNode = prev.strToStringNode;
        }

        @Specialization
        public RubyObject prepend(VirtualFrame frame, RubiniusByteArray self, RubyObject str) {
            return self.prepend((RubyString) strToStringNode.call(frame, str, "to_s", null));
        }
    }

    @CoreMethod(names = "utf8_char")
    public abstract static class Utf8CharNode extends CoreMethodNode {
        public Utf8CharNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public Utf8CharNode(Utf8CharNode prev) {
            super(prev);
        }

        @Specialization
        public RubyObject utf8_char(RubiniusByteArray self, RubyObject offset) {
            return self.utf8_char(this, offset);
        }
    }

    @CoreMethod(names = "reverse")
    public abstract static class ReverseNode extends CoreMethodNode {
        @Child
        protected DispatchHeadNode o_totalToIntNode;
        @Child
        protected DispatchHeadNode o_startToIntNode;

        public ReverseNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            o_startToIntNode = new DispatchHeadNode(context);
            o_totalToIntNode = new DispatchHeadNode(context);
        }

        public ReverseNode(ReverseNode prev) {
            super(prev);
            o_startToIntNode = prev.o_startToIntNode;
            o_totalToIntNode = prev.o_totalToIntNode;
        }

        @Specialization
        public RubyObject reverse(VirtualFrame frame, RubiniusByteArray self, RubyObject o_start, RubyObject o_total) {
            return self.reverse(TypeConversionUtils.convertToLong(this, o_startToIntNode, frame, o_start), TypeConversionUtils.convertToLong(this, o_totalToIntNode, frame, o_total));
        }
    }
}
