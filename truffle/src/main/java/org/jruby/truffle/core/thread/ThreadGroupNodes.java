package org.jruby.truffle.core.thread;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.objects.AllocateObjectNode;
import org.jruby.truffle.language.objects.IsFrozenNode;
import org.jruby.truffle.language.objects.IsFrozenNodeGen;

import java.util.ArrayList;
import java.util.List;

@CoreClass("ThreadGroup")
public abstract class ThreadGroupNodes {

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateObjectNode allocateObjectNode;

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNode.create();
        }

        @Specialization
        public DynamicObject allocateThreadGroup(DynamicObject rubyClass) {
            return allocateObjectNode.allocate(rubyClass, false);
        }

    }

    @CoreMethod(names = "enclose")
    public abstract static class EncloseNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject enclose(DynamicObject group) {
            Layouts.THREAD_GROUP.setEnclosed(group, true);
            return group;
        }

    }

    @CoreMethod(names = "enclosed?")
    public abstract static class EnclosedNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public boolean enclosed(DynamicObject group) {
            return Layouts.THREAD_GROUP.getEnclosed(group);
        }

    }

    @CoreMethod(names = "list")
    public abstract static class ListNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject list(DynamicObject group) {
            final Object[] threads = getContext().getThreadManager().getThreadList();
            List<Object> results = new ArrayList<>();
            for (Object thread : threads) {
                if (Layouts.THREAD.getThreadGroup((DynamicObject) thread) == group) {
                    results.add(thread);
                }
            }
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), results.toArray(), results.size());
        }

    }

    @CoreMethod(names = "add", required = 1)
    public abstract static class AddNode extends CoreMethodArrayArgumentsNode {

        @Child private IsFrozenNode isFrozenToNode;
        @Child private IsFrozenNode isFrozenFromNode;

        public AddNode(RubyContext context, SourceSection sourceSection) {
            isFrozenToNode = IsFrozenNodeGen.create(context, sourceSection, null);
            isFrozenFromNode = IsFrozenNodeGen.create(context, sourceSection, null);
        }

        @Specialization
        public DynamicObject add(DynamicObject toGroup, DynamicObject thread) {


            if (isFrozenToNode.executeIsFrozen(toGroup)) {
                throw new RaiseException(coreExceptions().threadErrorFrozenToThreadGroup(this));
            }

            if (Layouts.THREAD_GROUP.getEnclosed(toGroup)) {
                throw new RaiseException(coreExceptions().threadErrorToEnclosedThreadGroup(this));
            }

            final DynamicObject fromGroup = Layouts.THREAD.getThreadGroup(thread);

            if (fromGroup == null) {
                return nil();
            }

            if (isFrozenFromNode.executeIsFrozen(fromGroup)) {
                throw new RaiseException(coreExceptions().threadErrorFrozenFromThreadGroup(this));
            }

            if (Layouts.THREAD_GROUP.getEnclosed(fromGroup)) {
                throw new RaiseException(coreExceptions().threadErrorFromEnclosedThreadGroup(this));
            }
            Layouts.THREAD.setThreadGroup(thread, toGroup);
            return toGroup;
        }

    }


}
