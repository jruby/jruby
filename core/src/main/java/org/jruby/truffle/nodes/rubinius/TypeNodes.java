package org.jruby.truffle.nodes.rubinius;


import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.core.CoreClass;
import org.jruby.truffle.nodes.core.CoreMethod;
import org.jruby.truffle.nodes.core.CoreMethodNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;

@CoreClass(name = "Type")
public abstract class TypeNodes {
    @CoreMethod(isModuleFunction = true, names = "object_kind_of?", needsSelf = false)
    public abstract static class ObjKindOfPNode extends CoreMethodNode {
        public ObjKindOfPNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ObjKindOfPNode(ObjKindOfPNode prev) {
            super(prev);
        }

        @Specialization
        public boolean obj_kind_of_p(RubyBasicObject obj, RubyClass cls) {
            return obj.getLogicalClass() == cls;
        }
    }
}
