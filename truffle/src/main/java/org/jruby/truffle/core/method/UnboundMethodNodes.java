/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.method;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.UnaryCoreMethodNode;
import org.jruby.truffle.core.Hashing;
import org.jruby.truffle.core.module.ModuleOperations;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.Visibility;
import org.jruby.truffle.language.arguments.ArgumentDescriptorUtils;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.methods.CanBindMethodToModuleNode;
import org.jruby.truffle.language.methods.CanBindMethodToModuleNodeGen;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.language.objects.MetaClassNode;
import org.jruby.truffle.language.objects.MetaClassNodeGen;
import org.jruby.truffle.parser.ArgumentDescriptor;

@CoreClass("UnboundMethod")
public abstract class UnboundMethodNodes {

    @CoreMethod(names = "==", required = 1)
    public abstract static class EqualNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isRubyUnboundMethod(other)")
        boolean equal(DynamicObject self, DynamicObject other) {
            return Layouts.UNBOUND_METHOD.getMethod(self) == Layouts.UNBOUND_METHOD.getMethod(other) && Layouts.UNBOUND_METHOD.getOrigin(self) == Layouts.UNBOUND_METHOD.getOrigin(other);
        }

        @Specialization(guards = "!isRubyUnboundMethod(other)")
        boolean equal(DynamicObject self, Object other) {
            return false;
        }

    }

    @CoreMethod(names = "arity")
    public abstract static class ArityNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int arity(DynamicObject method) {
            return Layouts.UNBOUND_METHOD.getMethod(method).getSharedMethodInfo().getArity().getArityNumber();
        }

    }

    @CoreMethod(names = "bind", required = 1)
    public abstract static class BindNode extends CoreMethodArrayArgumentsNode {

        @Child private MetaClassNode metaClassNode = MetaClassNodeGen.create(null);
        @Child private CanBindMethodToModuleNode canBindMethodToModuleNode = CanBindMethodToModuleNodeGen.create(null, null);

        @Specialization
        public DynamicObject bind(DynamicObject unboundMethod, Object object,
                @Cached("create()") BranchProfile errorProfile) {
            final DynamicObject objectMetaClass = metaClass(object);

            if (!canBindMethodToModuleNode.executeCanBindMethodToModule(Layouts.UNBOUND_METHOD.getMethod(unboundMethod), objectMetaClass)) {
                errorProfile.enter();
                final DynamicObject declaringModule = Layouts.UNBOUND_METHOD.getMethod(unboundMethod).getDeclaringModule();
                if (RubyGuards.isSingletonClass(declaringModule)) {
                    throw new RaiseException(coreExceptions().typeError(
                            "singleton method called for a different object", this));
                } else {
                    throw new RaiseException(coreExceptions().typeError(
                            "bind argument must be an instance of " + Layouts.MODULE.getFields(declaringModule).getName(), this));
                }
            }

            return Layouts.METHOD.createMethod(coreLibrary().getMethodFactory(), object, Layouts.UNBOUND_METHOD.getMethod(unboundMethod));
        }

        protected DynamicObject metaClass(Object object) {
            return metaClassNode.executeMetaClass(object);
        }

    }

    @CoreMethod(names = "hash")
    public abstract static class HashNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public long hash(DynamicObject rubyMethod) {
            final InternalMethod method = Layouts.UNBOUND_METHOD.getMethod(rubyMethod);
            long h = Hashing.start(method.getDeclaringModule().hashCode());
            h = Hashing.update(h, Layouts.UNBOUND_METHOD.getOrigin(rubyMethod).hashCode());
            h = Hashing.update(h, method.getSharedMethodInfo().hashCode());
            return Hashing.end(h);
        }

    }

    @CoreMethod(names = "name")
    public abstract static class NameNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject name(DynamicObject unboundMethod) {
            return getSymbol(Layouts.UNBOUND_METHOD.getMethod(unboundMethod).getName());
        }

    }

    // TODO: We should have an additional method for this but we need to access it for #inspect.
    @CoreMethod(names = "origin", visibility = Visibility.PRIVATE)
    public abstract static class OriginNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject origin(DynamicObject unboundMethod) {
            return Layouts.UNBOUND_METHOD.getOrigin(unboundMethod);
        }

    }

    @CoreMethod(names = "owner")
    public abstract static class OwnerNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject owner(DynamicObject unboundMethod) {
            return Layouts.UNBOUND_METHOD.getMethod(unboundMethod).getDeclaringModule();
        }

    }

    @CoreMethod(names = "parameters")
    public abstract static class ParametersNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject parameters(DynamicObject method) {
            final ArgumentDescriptor[] argsDesc = Layouts.UNBOUND_METHOD.getMethod(method).getSharedMethodInfo().getArgumentDescriptors();

            return ArgumentDescriptorUtils.argumentDescriptorsToParameters(getContext(), argsDesc, true);
        }

    }

    @CoreMethod(names = "source_location")
    public abstract static class SourceLocationNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public Object sourceLocation(DynamicObject unboundMethod) {
            SourceSection sourceSection = Layouts.UNBOUND_METHOD.getMethod(unboundMethod).getSharedMethodInfo().getSourceSection();

            if (sourceSection.getSource() == null) {
                return nil();
            } else {
                DynamicObject file = createString(StringOperations.encodeRope(sourceSection.getSource().getName(), UTF8Encoding.INSTANCE));
                Object[] objects = new Object[]{file, sourceSection.getStartLine()};
                return createArray(objects, objects.length);
            }
        }

    }

    @CoreMethod(names = "super_method")
    public abstract static class SuperMethodNode extends CoreMethodArrayArgumentsNode {

        @Child MetaClassNode metaClassNode = MetaClassNode.create();

        @Specialization
        public DynamicObject superMethod(DynamicObject unboundMethod) {
            InternalMethod internalMethod = Layouts.UNBOUND_METHOD.getMethod(unboundMethod);
            DynamicObject origin = Layouts.UNBOUND_METHOD.getOrigin(unboundMethod);
            InternalMethod superMethod = ModuleOperations.lookupSuperMethod(internalMethod, origin);
            if (superMethod == null || superMethod.isUndefined()) {
                return nil();
            } else {
                return Layouts.UNBOUND_METHOD.createUnboundMethod(coreLibrary().getUnboundMethodFactory(),
                        superMethod.getDeclaringModule(), superMethod);
            }
        }

    }

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends UnaryCoreMethodNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            throw new RaiseException(coreExceptions().typeErrorAllocatorUndefinedFor(rubyClass, this));
        }

    }

}
