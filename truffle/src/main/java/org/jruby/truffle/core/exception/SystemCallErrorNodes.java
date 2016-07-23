package org.jruby.truffle.core.exception;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.Primitive;
import org.jruby.truffle.builtins.PrimitiveArrayArgumentsNode;
import org.jruby.truffle.language.objects.AllocateObjectNode;
import org.jruby.truffle.language.objects.AllocateObjectNodeGen;

@CoreClass("SystemCallError")
public abstract class SystemCallErrorNodes {

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateObjectNode allocateObjectNode;

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
        }

        @Specialization
        public DynamicObject allocateNameError(DynamicObject rubyClass) {
            return allocateObjectNode.allocate(rubyClass, nil(), null, nil());
        }

    }

    @CoreMethod(names = "errno")
    public abstract static class ErrnoNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public Object errno(DynamicObject self) {
            return Layouts.SYSTEM_CALL_ERROR.getErrno(self);
        }

    }

    @Primitive(name = "exception_set_errno")
    public abstract static class ErrnoSetNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        public Object setErrno(DynamicObject error, Object errno) {
            Layouts.SYSTEM_CALL_ERROR.setErrno(error, errno);
            return errno;
        }

    }

}
