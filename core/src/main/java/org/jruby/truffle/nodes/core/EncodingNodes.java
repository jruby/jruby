package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.SourceSection;
import com.oracle.truffle.api.dsl.Specialization;
import org.jcodings.Encoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.runtime.NilPlaceholder;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyEncoding;
import org.jruby.truffle.runtime.core.RubyString;

@CoreClass(name = "Encoding")
public abstract class EncodingNodes {

    @CoreMethod(names = {"==", "==="}, minArgs = 1, maxArgs = 1)
    public abstract static class EqualNode extends CoreMethodNode {

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EqualNode(EqualNode prev) {
            super(prev);
        }

        @Specialization
        public boolean equal(@SuppressWarnings("unused") RubyString a, @SuppressWarnings("unused") NilPlaceholder b) {
            notDesignedForCompilation();

            return false;
        }

        @Specialization
        public boolean equal(RubyEncoding a, RubyEncoding b) {
            notDesignedForCompilation();

            return a.compareTo(b);
        }

    }

    @CoreMethod(names = "default_external", isModuleMethod = true, needsSelf = false, maxArgs = 0)
    public abstract static class DefaultExternalNode extends CoreMethodNode {

        public DefaultExternalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DefaultExternalNode(DefaultExternalNode prev) {
            super(prev);
        }

        @Specialization
        public RubyEncoding defaultExternal() {
            notDesignedForCompilation();

            Encoding encoding = getContext().getRuntime().getDefaultExternalEncoding();

            if (encoding == null) {
                encoding = UTF8Encoding.INSTANCE;
            }

            return new RubyEncoding(getContext().getCoreLibrary().getEncodingClass(), encoding);
        }

    }

    @CoreMethod(names = "default_internal", isModuleMethod = true, needsSelf = false, maxArgs = 0)
    public abstract static class DefaultInternalNode extends CoreMethodNode {

        public DefaultInternalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DefaultInternalNode(DefaultInternalNode prev) {
            super(prev);
        }

        @Specialization
        public RubyEncoding defaultInternal() {
            notDesignedForCompilation();

            Encoding encoding = getContext().getRuntime().getDefaultInternalEncoding();

            if (encoding == null) {
                encoding = UTF8Encoding.INSTANCE;
            }

            return new RubyEncoding(getContext().getCoreLibrary().getEncodingClass(), encoding);
        }

    }

    @CoreMethod(names = "find", isModuleMethod = true, needsSelf = false, maxArgs = 1, minArgs = 1)
    public abstract static class FindNode extends CoreMethodNode {

        public FindNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public FindNode(FindNode prev) {
            super(prev);
        }

        @Specialization
        public Object find(RubyString name) {
            notDesignedForCompilation();

            // TODO(CS): isn't this a JRuby object?

            return RubyEncoding.findEncodingByName(name);
        }

    }

}
