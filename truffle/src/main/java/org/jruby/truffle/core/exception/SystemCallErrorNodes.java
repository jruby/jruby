package org.jruby.truffle.core.exception;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.NonStandard;
import org.jruby.truffle.language.RubyNode;

@CoreClass("SystemCallError")
public abstract class SystemCallErrorNodes {

    @CoreMethod(names = "errno")
    public abstract static class ErrnoNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public Object errno(DynamicObject self) {
            return Layouts.SYSTEM_CALL_ERROR.getErrno(self);
        }

    }

    @NonStandard
    @NodeChildren({
        @NodeChild(type = RubyNode.class, value = "self"),
        @NodeChild(type = RubyNode.class, value = "errno")
    })
    public abstract static class InternalSetErrnoNode extends RubyNode {

        @Specialization
        public Object setErrno(DynamicObject error, int errno) {
            Layouts.SYSTEM_CALL_ERROR.setErrno(error, errno);
            return errno;
        }

        @Specialization(guards = "isNil(errno)")
        public Object setErrno(DynamicObject error, DynamicObject errno) {
            Layouts.SYSTEM_CALL_ERROR.setErrno(error, errno);
            return errno;
        }

    }

}
