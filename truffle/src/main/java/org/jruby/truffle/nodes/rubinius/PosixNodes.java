/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.rubinius;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.platform.Platform;
import org.jruby.truffle.nodes.core.CoreClass;
import org.jruby.truffle.nodes.core.CoreMethod;
import org.jruby.truffle.nodes.core.CoreMethodNode;
import org.jruby.truffle.nodes.objectstorage.ReadHeadObjectFieldNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.rubinius.RubiniusByteArray;
import org.jruby.util.unsafe.UnsafeHolder;
import sun.misc.Unsafe;

import java.nio.charset.StandardCharsets;

@CoreClass(name = "Rubinius::FFI::Platform::POSIX")
public abstract class PosixNodes {

    @CoreMethod(names = "getegid", isModuleFunction = true)
    public abstract static class GetEGIDNode extends CoreMethodNode {

        public GetEGIDNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int getEGID() {
            return getContext().getPosix().getegid();
        }

    }

    @CoreMethod(names = "geteuid", isModuleFunction = true)
    public abstract static class GetEUIDNode extends CoreMethodNode {

        public GetEUIDNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int getEUID() {
            return getContext().getPosix().geteuid();
        }

    }

    @CoreMethod(names = "getgid", isModuleFunction = true)
    public abstract static class GetGIDNode extends CoreMethodNode {

        public GetGIDNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int getGID() {
            return getContext().getPosix().getgid();
        }

    }

    @CoreMethod(names = "getgroups", isModuleFunction = true, required = 2)
    public abstract static class GetGroupsNode extends PointerPrimitiveNodes.ReadAddressPrimitiveNode {

        public GetGroupsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int getGroups(int max, RubyBasicObject pointer) {
            final long[] groups = Platform.getPlatform().getGroups(null);

            final long address = getAddress(pointer);

            for (int n = 0; n < groups.length && n < max; n++) {
                UnsafeHolder.U.putInt(address + n * Unsafe.ARRAY_LONG_INDEX_SCALE, (int) groups[n]);
            }

            return groups.length;
        }

    }

    @CoreMethod(names = "getuid", isModuleFunction = true)
    public abstract static class GetUIDNode extends CoreMethodNode {

        public GetUIDNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int getUID() {
            return getContext().getPosix().getuid();
        }

    }

    @CoreMethod(names = "memset", isModuleFunction = true, required = 3)
    public abstract static class MemsetNode extends PointerPrimitiveNodes.ReadAddressPrimitiveNode {

        public MemsetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject memset(RubyBasicObject pointer, int c, int length) {
            return memset(pointer, c, (long) length);
        }

        @Specialization
        public RubyBasicObject memset(RubyBasicObject pointer, int c, long length) {
            final long address = getAddress(pointer);
            UnsafeHolder.U.setMemory(address, length, (byte) c);
            return pointer;
        }

    }

    @CoreMethod(names = "unlink", isModuleFunction = true, required = 1)
    public abstract static class UnlinkNode extends PointerPrimitiveNodes.ReadAddressPrimitiveNode {

        public UnlinkNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int unlink(RubyString path) {
            return getContext().getPosix().unlink(path.toString());
        }

    }

    @CoreMethod(names = "mkdir", isModuleFunction = true, required = 2)
    public abstract static class MkdirNode extends PointerPrimitiveNodes.ReadAddressPrimitiveNode {

        public MkdirNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int mkdir(RubyString path, int mode) {
            return getContext().getPosix().mkdir(path.toString(), mode);
        }

    }

    @CoreMethod(names = "chdir", isModuleFunction = true, required = 1)
    public abstract static class ChdirNode extends PointerPrimitiveNodes.ReadAddressPrimitiveNode {

        public ChdirNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int chdir(RubyString path) {
            final String pathString = path.toString();

            final int result = getContext().getPosix().chdir(pathString);

            if (result == 0) {
                getContext().getRuntime().setCurrentDirectory(pathString);
            }

            return result;
        }

    }

    @CoreMethod(names = "rmdir", isModuleFunction = true, required = 1)
    public abstract static class RmdirNode extends PointerPrimitiveNodes.ReadAddressPrimitiveNode {

        public RmdirNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int rmdir(RubyString path) {
            return getContext().getPosix().rmdir(path.toString());
        }

    }

    @CoreMethod(names = "getcwd", isModuleFunction = true, required = 2)
    public abstract static class GetcwdNode extends PointerPrimitiveNodes.ReadAddressPrimitiveNode {

        public GetcwdNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyString getcwd(RubyString resultPath, int maxSize) {
            // We just ignore maxSize - I think this is ok

            final String path = getContext().getRuntime().getCurrentDirectory();
            resultPath.getByteList().replace(path.getBytes(StandardCharsets.UTF_8));
            return resultPath;
        }

    }

    @CoreMethod(names = "errno", isModuleFunction = true)
    public abstract static class ErrnoNode extends CoreMethodNode {

        public ErrnoNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int errno() {
            return getContext().getPosix().errno();
        }

    }

}
