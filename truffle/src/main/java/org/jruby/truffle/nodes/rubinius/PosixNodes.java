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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import jnr.constants.platform.Fcntl;
import jnr.ffi.Pointer;
import org.jruby.RubyEncoding;
import org.jruby.platform.Platform;
import org.jruby.truffle.nodes.core.CoreClass;
import org.jruby.truffle.nodes.core.CoreMethod;
import org.jruby.truffle.nodes.core.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.rubinius.RubiniusConfiguration;

import java.nio.charset.StandardCharsets;

@CoreClass(name = "Rubinius::FFI::Platform::POSIX")
public abstract class PosixNodes {

    @CoreMethod(names = "access", isModuleFunction = true, required = 2)
    public abstract static class AccessNode extends CoreMethodArrayArgumentsNode {

        public AccessNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int access(RubyString path, int mode) {
            final String pathString = RubyEncoding.decodeUTF8(path.getByteList().getUnsafeBytes(), path.getByteList().getBegin(), path.getByteList().getRealSize());
            return posix().access(pathString, mode);
        }

    }

    @CoreMethod(names = "chmod", isModuleFunction = true, required = 2)
    public abstract static class ChmodNode extends CoreMethodArrayArgumentsNode {

        public ChmodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int chmod(RubyString path, int mode) {
            return posix().chmod(path.toString(), mode);
        }

    }

    @CoreMethod(names = "chown", isModuleFunction = true, required = 3, lowerFixnumParameters = {1, 2})
    public abstract static class ChownNode extends CoreMethodArrayArgumentsNode {

        public ChownNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int chown(RubyString path, int owner, int group) {
            return posix().chown(path.toString(), owner, group);
        }

    }

    @CoreMethod(names = "dup", isModuleFunction = true, required = 1)
    public abstract static class DupNode extends CoreMethodArrayArgumentsNode {

        public DupNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int dup(int descriptor) {
            return posix().dup(descriptor);
        }

    }

    @CoreMethod(names = "fchmod", isModuleFunction = true, required = 2)
    public abstract static class FchmodNode extends CoreMethodArrayArgumentsNode {

        public FchmodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int fchmod(int one, int mode) {
            return posix().fchmod(one, mode);
        }

    }


    @CoreMethod(names = "fsync", isModuleFunction = true, required = 1)
    public abstract static class FsyncNode extends CoreMethodArrayArgumentsNode {

        public FsyncNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int fsync(int descriptor) {
            return posix().fsync(descriptor);
        }

    }


    @CoreMethod(names = "fchown", isModuleFunction = true, required = 3)
    public abstract static class FchownNode extends CoreMethodArrayArgumentsNode {

        public FchownNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int fchown(int descriptor, int owner, int group) {
            return posix().fchown(descriptor, owner, group);
        }

    }

    @CoreMethod(names = "getegid", isModuleFunction = true)
    public abstract static class GetEGIDNode extends CoreMethodArrayArgumentsNode {

        public GetEGIDNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int getEGID() {
            return posix().getegid();
        }

    }

    @CoreMethod(names = "geteuid", isModuleFunction = true)
    public abstract static class GetEUIDNode extends CoreMethodArrayArgumentsNode {

        public GetEUIDNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int getEUID() {
            return posix().geteuid();
        }

    }

    @CoreMethod(names = "getgid", isModuleFunction = true)
    public abstract static class GetGIDNode extends CoreMethodArrayArgumentsNode {

        public GetGIDNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int getGID() {
            return posix().getgid();
        }

    }

    @CoreMethod(names = "getgroups", isModuleFunction = true, required = 2)
    public abstract static class GetGroupsNode extends CoreMethodArrayArgumentsNode {

        public GetGroupsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int getGroups(int max, RubyBasicObject pointer) {
            final long[] groups = Platform.getPlatform().getGroups(null);

            final Pointer pointerValue = PointerPrimitiveNodes.getPointer(pointer);

            for (int n = 0; n < groups.length && n < max; n++) {
                // TODO CS 16-May-15 this is platform dependent
                pointerValue.putInt(4 * n, (int) groups[n]);

            }

            return groups.length;
        }

    }

    @CoreMethod(names = "getuid", isModuleFunction = true)
    public abstract static class GetUIDNode extends CoreMethodArrayArgumentsNode {

        public GetUIDNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int getUID() {
            return posix().getuid();
        }

    }

    @CoreMethod(names = "memset", isModuleFunction = true, required = 3)
    public abstract static class MemsetNode extends CoreMethodArrayArgumentsNode {

        public MemsetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject memset(RubyBasicObject pointer, int c, int length) {
            return memset(pointer, c, (long) length);
        }

        @Specialization
        public RubyBasicObject memset(RubyBasicObject pointer, int c, long length) {
            PointerPrimitiveNodes.getPointer(pointer).setMemory(0, length, (byte) c);
            return pointer;
        }

    }

    @CoreMethod(names = "readlink", isModuleFunction = true, required = 3)
    public abstract static class ReadlinkNode extends CoreMethodArrayArgumentsNode {

        public ReadlinkNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int readlink(RubyString path, RubyBasicObject pointer, int bufsize) {
            final String pathString = RubyEncoding.decodeUTF8(path.getByteList().getUnsafeBytes(), path.getByteList().getBegin(), path.getByteList().getRealSize());

            final byte[] buffer = new byte[bufsize];

            final int result = posix().readlink(pathString, buffer, bufsize);
            if (result == -1) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().errnoError(posix().errno(), this));
            }

            PointerPrimitiveNodes.getPointer(pointer).put(0, buffer, 0, buffer.length);

            return result;
        }

    }

    @CoreMethod(names = "link", isModuleFunction = true, required = 2)
    public abstract static class LinkNode extends CoreMethodArrayArgumentsNode {

        public LinkNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int link(RubyString path, RubyString other) {
            final String pathString = RubyEncoding.decodeUTF8(path.getByteList().getUnsafeBytes(), path.getByteList().getBegin(), path.getByteList().getRealSize());
            final String otherString = RubyEncoding.decodeUTF8(other.getByteList().getUnsafeBytes(), other.getByteList().getBegin(), other.getByteList().getRealSize());
            return posix().link(pathString, otherString);
        }

    }

    @CoreMethod(names = "unlink", isModuleFunction = true, required = 1)
    public abstract static class UnlinkNode extends CoreMethodArrayArgumentsNode {

        public UnlinkNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int unlink(RubyString path) {
            return posix().unlink(RubyEncoding.decodeUTF8(path.getByteList().getUnsafeBytes(), path.getByteList().getBegin(), path.getByteList().getRealSize()));
        }

    }

    @CoreMethod(names = "umask", isModuleFunction = true, required = 1)
    public abstract static class UmaskNode extends CoreMethodArrayArgumentsNode {

        public UmaskNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int umask(int mask) {
            return posix().umask(mask);
        }

    }

    @CoreMethod(names = "mkdir", isModuleFunction = true, required = 2)
    public abstract static class MkdirNode extends CoreMethodArrayArgumentsNode {

        public MkdirNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int mkdir(RubyString path, int mode) {
            return posix().mkdir(path.toString(), mode);
        }

    }

    @CoreMethod(names = "chdir", isModuleFunction = true, required = 1)
    public abstract static class ChdirNode extends CoreMethodArrayArgumentsNode {

        public ChdirNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int chdir(RubyString path) {
            final String pathString = path.toString();

            final int result = posix().chdir(pathString);

            if (result == 0) {
                getContext().getRuntime().setCurrentDirectory(pathString);
            }

            return result;
        }

    }

    @CoreMethod(names = "flock", isModuleFunction = true, required = 2, lowerFixnumParameters = {0, 1})
    public abstract static class FlockNode extends CoreMethodArrayArgumentsNode {

        public FlockNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int flock(int fd, int constant) {
            return posix().flock(fd, constant);
        }

    }

    @CoreMethod(names = "major", isModuleFunction = true, required = 1, lowerFixnumParameters = 0)
    public abstract static class MajorNode extends CoreMethodArrayArgumentsNode {

        public MajorNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int major(int dev) {
            return (dev >> 24) & 255;
        }

    }

    @CoreMethod(names = "minor", isModuleFunction = true, required = 1, lowerFixnumParameters = 0)
    public abstract static class MinorNode extends CoreMethodArrayArgumentsNode {

        public MinorNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int minor(int dev) {
            return (dev & 16777215);
        }

    }

    @CoreMethod(names = "rename", isModuleFunction = true, required = 2)
    public abstract static class RenameNode extends CoreMethodArrayArgumentsNode {

        public RenameNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int rename(RubyString path, RubyString other) {
            final String pathString = RubyEncoding.decodeUTF8(path.getByteList().getUnsafeBytes(), path.getByteList().getBegin(), path.getByteList().getRealSize());
            final String otherString = RubyEncoding.decodeUTF8(other.getByteList().getUnsafeBytes(), other.getByteList().getBegin(), other.getByteList().getRealSize());
            return posix().rename(pathString, otherString);
        }

    }

    @CoreMethod(names = "rmdir", isModuleFunction = true, required = 1)
    public abstract static class RmdirNode extends CoreMethodArrayArgumentsNode {

        public RmdirNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int rmdir(RubyString path) {
            return posix().rmdir(path.toString());
        }

    }

    @CoreMethod(names = "getcwd", isModuleFunction = true, required = 2)
    public abstract static class GetcwdNode extends CoreMethodArrayArgumentsNode {

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
    public abstract static class ErrnoNode extends CoreMethodArrayArgumentsNode {

        public ErrnoNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int errno() {
            return posix().errno();
        }

    }

    @CoreMethod(names = "fcntl", isModuleFunction = true, required = 3)
    public abstract static class FcntlNode extends CoreMethodArrayArgumentsNode {

        public FcntlNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNil(nil)")
        public int fcntl(int fd, int fcntl, Object nil) {
            return posix().fcntl(fd, Fcntl.valueOf(fcntl));
        }

        @Specialization
        public int fcntl(int fd, int fcntl, int arg) {
            return posix().fcntlInt(fd, Fcntl.valueOf(fcntl), arg);
        }

    }

    @CoreMethod(names = "isatty", isModuleFunction = true, required = 1)
    public abstract static class IsATTYNode extends CoreMethodArrayArgumentsNode {

        public IsATTYNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int isATTY(int fd) {
            return posix().libc().isatty(fd);
        }

    }

    @CoreMethod(names = "symlink", isModuleFunction = true, required = 2)
    public abstract static class SymlinkNode extends CoreMethodArrayArgumentsNode {

        public SymlinkNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int symlink(RubyString first, RubyString second) {
            return posix().symlink(first.toString(), second.toString());
        }

    }

    // TODO CS-15-May 15 we're missing a lot of guards for things like Pointer here

    @CoreMethod(names = "_getaddrinfo", isModuleFunction = true, required = 4)
    public abstract static class GetAddrInfoNode extends CoreMethodArrayArgumentsNode {

        public GetAddrInfoNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int getaddrinfo(RubyString hostName, RubyString serviceName, RubyBasicObject hintsPointer, RubyBasicObject resultsPointer) {
            return nativeSockets().getaddrinfo(
                    hostName.getByteList(),
                    serviceName.getByteList(),
                    PointerPrimitiveNodes.getPointer(hintsPointer),
                    PointerPrimitiveNodes.getPointer(resultsPointer));
        }

    }

    @CoreMethod(names = "freeaddrinfo", isModuleFunction = true, required = 1)
    public abstract static class FreeAddrInfoNode extends CoreMethodArrayArgumentsNode {

        public FreeAddrInfoNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject freeaddrinfo(RubyBasicObject addrInfo) {
            nativeSockets().freeaddrinfo(PointerPrimitiveNodes.getPointer(addrInfo));
            return nil();
        }

    }

    @CoreMethod(names = "_getnameinfo", isModuleFunction = true, required = 7)
    public abstract static class GetNameInfoNode extends CoreMethodArrayArgumentsNode {

        public GetNameInfoNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int getnameinfo(RubyBasicObject sa, int salen, RubyBasicObject host, int hostlen, RubyBasicObject serv, int servlen, int flags) {
            return nativeSockets().getnameinfo(
                    PointerPrimitiveNodes.getPointer(sa),
                    salen,
                    PointerPrimitiveNodes.getPointer(host),
                    hostlen,
                    PointerPrimitiveNodes.getPointer(serv),
                    servlen,
                    flags);
        }

    }

    @CoreMethod(names = "socket", isModuleFunction = true, required = 3)
    public abstract static class SocketNode extends CoreMethodArrayArgumentsNode {

        public SocketNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int getnameinfo(int domain, int type, int protocol) {
            return nativeSockets().socket(domain, type, protocol);
        }

    }

    @CoreMethod(names = "setsockopt", isModuleFunction = true, required = 5, lowerFixnumParameters = {0, 1, 2, 4})
    public abstract static class SetSockOptNode extends CoreMethodArrayArgumentsNode {

        public SetSockOptNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int setsockopt(int socket, int level, int optionName, RubyBasicObject optionValue, int optionLength) {
            return nativeSockets().setsockopt(socket, level, optionName, PointerPrimitiveNodes.getPointer(optionValue), optionLength);
        }

    }

    @CoreMethod(names = "_bind", isModuleFunction = true, required = 3)
    public abstract static class BindNode extends CoreMethodArrayArgumentsNode {

        public BindNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int bind(int socket, RubyBasicObject address, int addressLength) {
            return nativeSockets().bind(socket, PointerPrimitiveNodes.getPointer(address), addressLength);
        }

    }

    @CoreMethod(names = "listen", isModuleFunction = true, required = 2)
    public abstract static class ListenNode extends CoreMethodArrayArgumentsNode {

        public ListenNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int listen(int socket, int backlog) {
            return nativeSockets().listen(socket, backlog);
        }

    }

}
