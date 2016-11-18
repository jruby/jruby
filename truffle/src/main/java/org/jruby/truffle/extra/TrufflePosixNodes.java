/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.extra;

import com.kenai.jffi.Platform;
import com.kenai.jffi.Platform.OS;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.sun.security.auth.module.UnixSystem;
import jnr.constants.platform.Fcntl;
import jnr.ffi.Pointer;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.rope.RopeNodes;
import org.jruby.truffle.core.rope.RopeNodesFactory;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.core.time.GetTimeZoneNode;
import org.jruby.truffle.extra.ffi.PointerPrimitiveNodes;
import org.jruby.truffle.language.SnippetNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.platform.UnsafeGroup;
import org.jruby.truffle.platform.signal.Signal;

import static org.jruby.truffle.core.string.StringOperations.decodeUTF8;

@CoreClass("Truffle::POSIX")
public abstract class TrufflePosixNodes {

    private static void invalidateENV(String name) {
        if (name.equals("TZ")) {
            GetTimeZoneNode.invalidateTZ();
        }
    }

    @CoreMethod(names = "access", isModuleFunction = true, required = 2, lowerFixnum = 2, unsafe = UnsafeGroup.IO)
    public abstract static class AccessNode extends CoreMethodArrayArgumentsNode {

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = "isRubyString(path)")
        public int access(DynamicObject path, int mode) {
            final String pathString = decodeUTF8(path);
            return posix().access(pathString, mode);
        }

    }

    @CoreMethod(names = "chmod", isModuleFunction = true, required = 2, lowerFixnum = 2, unsafe = UnsafeGroup.IO)
    public abstract static class ChmodNode extends CoreMethodArrayArgumentsNode {

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = "isRubyString(path)")
        public int chmod(DynamicObject path, int mode) {
            return posix().chmod(decodeUTF8(path), mode);
        }

    }

    @CoreMethod(names = "chown", isModuleFunction = true, required = 3, lowerFixnum = { 1, 2 }, unsafe = UnsafeGroup.IO)
    public abstract static class ChownNode extends CoreMethodArrayArgumentsNode {

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = "isRubyString(path)")
        public int chown(DynamicObject path, int owner, int group) {
            return posix().chown(decodeUTF8(path), owner, group);
        }

    }

    @CoreMethod(names = "dup", isModuleFunction = true, required = 1, lowerFixnum = 1, unsafe = UnsafeGroup.IO)
    public abstract static class DupNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int dup(int descriptor) {
            return posix().dup(descriptor);
        }

    }

    @CoreMethod(names = "fchmod", isModuleFunction = true, required = 2, lowerFixnum = {1, 2}, unsafe = UnsafeGroup.IO)
    public abstract static class FchmodNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int fchmod(int one, int mode) {
            return posix().fchmod(one, mode);
        }

    }


    @CoreMethod(names = "fsync", isModuleFunction = true, required = 1, lowerFixnum = 1, unsafe = UnsafeGroup.IO)
    public abstract static class FsyncNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int fsync(int descriptor) {
            return posix().fsync(descriptor);
        }

    }


    @CoreMethod(names = "fchown", isModuleFunction = true, required = 3, lowerFixnum = {1, 2, 3}, unsafe = UnsafeGroup.IO)
    public abstract static class FchownNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int fchown(int descriptor, int owner, int group) {
            return posix().fchown(descriptor, owner, group);
        }

    }

    @CoreMethod(names = "getegid", isModuleFunction = true, unsafe = UnsafeGroup.PROCESSES)
    public abstract static class GetEGIDNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int getEGID() {
            return posix().getegid();
        }

    }

    @CoreMethod(names = "getenv", isModuleFunction = true, required = 1, unsafe = {UnsafeGroup.MEMORY, UnsafeGroup.PROCESSES})
    public abstract static class GetenvNode extends CoreMethodArrayArgumentsNode {

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = "isRubyString(name)")
        public DynamicObject getenv(DynamicObject name) {
            Object result = posix().getenv(decodeUTF8(name));

            if (result == null) {
                return nil();
            }

            return createString(StringOperations.encodeRope((String) result, UTF8Encoding.INSTANCE));
        }
    }

    @CoreMethod(names = "geteuid", isModuleFunction = true, unsafe = UnsafeGroup.PROCESSES)
    public abstract static class GetEUIDNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int getEUID() {
            return posix().geteuid();
        }

    }

    @CoreMethod(names = "getgid", isModuleFunction = true, unsafe = UnsafeGroup.PROCESSES)
    public abstract static class GetGIDNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int getGID() {
            return posix().getgid();
        }

    }

    @CoreMethod(names = "getgroups", isModuleFunction = true, required = 2, lowerFixnum = 1, unsafe = {UnsafeGroup.MEMORY, UnsafeGroup.PROCESSES})
    public abstract static class GetGroupsNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = "isNil(pointer)")
        public int getGroupsNil(int max, DynamicObject pointer) {
            return getGroups().length;
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyPointer(pointer)")
        public int getGroups(int max, DynamicObject pointer) {
            final Pointer pointerValue = Layouts.POINTER.getPointer(pointer);
            final long[] groups = getGroups();
            for (int n = 0; n < groups.length; n++) {
                pointerValue.putInt(4 * n, (int) groups[n]);
            }
            return groups.length;
        }

        @TruffleBoundary
        private static long[] getGroups() {
            return new UnixSystem().getGroups();
        }
    }

    @CoreMethod(names = "getrlimit", isModuleFunction = true, required = 2, lowerFixnum = 1, unsafe = {UnsafeGroup.PROCESSES, UnsafeGroup.MEMORY})
    public abstract static class GetRLimitNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary(throwsControlFlowException = true)
        @Specialization(guards = "isRubyPointer(pointer)")
        public int getrlimit(int resource, DynamicObject pointer) {
            final int result = posix().getrlimit(resource, Layouts.POINTER.getPointer(pointer));

            if (result == -1) {
                throw new RaiseException(coreExceptions().errnoError(posix().errno(), this));
            }

            return result;
        }

    }

    @CoreMethod(names = "getuid", isModuleFunction = true, unsafe = UnsafeGroup.PROCESSES)
    public abstract static class GetUIDNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int getUID() {
            return posix().getuid();
        }

    }

    @CoreMethod(names = "memset", isModuleFunction = true, required = 3, lowerFixnum = {2, 3}, unsafe = UnsafeGroup.MEMORY)
    public abstract static class MemsetNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isRubyPointer(pointer)")
        public DynamicObject memset(DynamicObject pointer, int c, int length) {
            return memset(pointer, c, (long) length);
        }

        @Specialization(guards = "isRubyPointer(pointer)")
        public DynamicObject memset(DynamicObject pointer, int c, long length) {
            Layouts.POINTER.getPointer(pointer).setMemory(0, length, (byte) c);
            return pointer;
        }

    }

    @CoreMethod(names = "mkfifo", isModuleFunction = true, required = 2, lowerFixnum = 2, unsafe = UnsafeGroup.IO)
    public abstract static class MkfifoNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = "isRubyString(path)")
        public int mkfifo(DynamicObject path, int mode) {
            return posix().mkfifo(StringOperations.getString(path), mode);
        }

    }

    @CoreMethod(names = "readlink", isModuleFunction = true, required = 3, lowerFixnum = 3, unsafe = UnsafeGroup.IO)
    public abstract static class ReadlinkNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary(throwsControlFlowException = true)
        @Specialization(guards = {"isRubyString(path)", "isRubyPointer(pointer)"})
        public int readlink(DynamicObject path, DynamicObject pointer, int bufsize) {
            final int result = posix().readlink(decodeUTF8(path), Layouts.POINTER.getPointer(pointer), bufsize);
            if (result == -1) {
                throw new RaiseException(coreExceptions().errnoError(posix().errno(), this));
            }

            return result;
        }

    }

    @CoreMethod(names = "setenv", isModuleFunction = true, required = 3, lowerFixnum = 3, unsafe = UnsafeGroup.PROCESSES)
    public abstract static class SetenvNode extends CoreMethodArrayArgumentsNode {

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = { "isRubyString(name)", "isRubyString(value)" })
        public int setenv(DynamicObject name, DynamicObject value, int overwrite) {
            final String nameString = decodeUTF8(name);
            invalidateENV(nameString);
            return posix().setenv(nameString, decodeUTF8(value), overwrite);
        }

    }

    @CoreMethod(names = "lchmod", isModuleFunction = true, required = 2, lowerFixnum = 2, unsafe = UnsafeGroup.IO)
    public abstract static class LchmodNode extends CoreMethodArrayArgumentsNode {

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = "isRubyString(path)")
        public int lchmod(DynamicObject path, int mode) {
            return posix().lchmod(decodeUTF8(path), mode);
        }

    }

    @CoreMethod(names = "link", isModuleFunction = true, required = 2, unsafe = UnsafeGroup.IO)
    public abstract static class LinkNode extends CoreMethodArrayArgumentsNode {

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = {"isRubyString(path)", "isRubyString(other)"})
        public int link(DynamicObject path, DynamicObject other) {
            return posix().link(decodeUTF8(path), decodeUTF8(other));
        }

    }

    @CoreMethod(names = "unlink", isModuleFunction = true, required = 1, unsafe = UnsafeGroup.IO)
    public abstract static class UnlinkNode extends CoreMethodArrayArgumentsNode {

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = "isRubyString(path)")
        public int unlink(DynamicObject path) {
            return posix().unlink(decodeUTF8(path));
        }

    }

    @CoreMethod(names = "umask", isModuleFunction = true, required = 1, lowerFixnum = 1, unsafe = UnsafeGroup.PROCESSES)
    public abstract static class UmaskNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int umask(int mask) {
            return posix().umask(mask);
        }

    }

    @CoreMethod(names = "unsetenv", isModuleFunction = true, required = 1, unsafe = UnsafeGroup.PROCESSES)
    public abstract static class UnsetenvNode extends CoreMethodArrayArgumentsNode {

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = "isRubyString(name)")
        public int unsetenv(DynamicObject name) {
            final String nameString = decodeUTF8(name);
            invalidateENV(nameString);
            return posix().unsetenv(nameString);
        }

    }

    @CoreMethod(names = "utimes", isModuleFunction = true, required = 2, unsafe = {UnsafeGroup.PROCESSES, UnsafeGroup.MEMORY})
    public abstract static class UtimesNode extends CoreMethodArrayArgumentsNode {

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = {"isRubyString(path)", "isRubyPointer(pointer)"})
        public int utimes(DynamicObject path, DynamicObject pointer) {
            final int result = posix().utimes(decodeUTF8(path), Layouts.POINTER.getPointer(pointer));
            if (result == -1) {
                throw new RaiseException(coreExceptions().errnoError(posix().errno(), this));
            }

            return result;
        }

    }

    @CoreMethod(names = "mkdir", isModuleFunction = true, required = 2, lowerFixnum = 1, unsafe = UnsafeGroup.IO)
    public abstract static class MkdirNode extends CoreMethodArrayArgumentsNode {

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = "isRubyString(path)")
        public int mkdir(DynamicObject path, int mode) {
            return posix().mkdir(path.toString(), mode);
        }

    }

    @CoreMethod(names = "chdir", isModuleFunction = true, required = 1, unsafe = UnsafeGroup.IO)
    public abstract static class ChdirNode extends CoreMethodArrayArgumentsNode {

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = "isRubyString(path)")
        public int chdir(DynamicObject path) {
            final String pathString = path.toString();

            final int result = posix().chdir(pathString);

            if (result == 0) {
                final String cwd = posix().getcwd();
                getContext().setCurrentDirectory(cwd);
            }

            return result;
        }

    }

    @CoreMethod(names = "getpriority", isModuleFunction = true, required = 2, lowerFixnum = { 1, 2 }, unsafe = UnsafeGroup.PROCESSES)
    public abstract static class GetPriorityNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int getpriority(int kind, int id) {
            return posix().getpriority(kind, id);
        }

    }

    @CoreMethod(names = "setgid", isModuleFunction = true, required = 1, lowerFixnum = 1, unsafe = UnsafeGroup.PROCESSES)
    public abstract static class SetgidNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int setgid(int gid) {
            return posix().setgid(gid);
        }

    }

    @CoreMethod(names = "setpgid", isModuleFunction = true, required = 2, lowerFixnum = {1, 2}, unsafe = UnsafeGroup.PROCESSES)
    public abstract static class SetpgidNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int setpgid(int pid, int pgid) {
            return posix().setpgid(pid, pgid);
        }

    }

    @CoreMethod(names = "setpriority", isModuleFunction = true, required = 3, lowerFixnum = { 1, 2, 3 }, unsafe = UnsafeGroup.PROCESSES)
    public abstract static class SetPriorityNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int setpriority(int kind, int id, int priority) {
            return posix().setpriority(kind, id, priority);
        }

    }

    @CoreMethod(names = "setresuid", isModuleFunction = true, required = 3, lowerFixnum = { 1, 2, 3 }, unsafe = UnsafeGroup.PROCESSES)
    public abstract static class SetResuidNode extends CoreMethodArrayArgumentsNode {

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public int setresuid(int uid, int id, int priority) {
            throw new RaiseException(coreExceptions().notImplementedError("setresuid", this));
        }

    }

    @CoreMethod(names = "seteuid", isModuleFunction = true, required = 1, lowerFixnum = 1, unsafe = UnsafeGroup.PROCESSES)
    public abstract static class SetEuidNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int seteuid(int uid) {
            return posix().seteuid(uid);
        }

    }

    @CoreMethod(names = "setreuid", isModuleFunction = true, required = 2, lowerFixnum = { 1, 2 }, unsafe = UnsafeGroup.PROCESSES)
    public abstract static class SetReuidNode extends CoreMethodArrayArgumentsNode {

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public int setreuid(int uid, int id) {
            throw new RaiseException(coreExceptions().notImplementedError("setreuid", this));
        }

    }

    @CoreMethod(names = "setrlimit", isModuleFunction = true, required = 2, lowerFixnum = 1, unsafe = UnsafeGroup.PROCESSES)
    public abstract static class SetRLimitNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isRubyPointer(pointer)")
        public int setrlimit(int resource, DynamicObject pointer,
                @Cached("create()") BranchProfile errorProfile) {
            final int result = posix().setrlimit(resource, Layouts.POINTER.getPointer(pointer));

            if (result == -1) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().errnoError(posix().errno(), this));
            }

            return result;
        }

    }

    @CoreMethod(names = "setruid", isModuleFunction = true, required = 1, lowerFixnum = 1, unsafe = UnsafeGroup.PROCESSES)
    public abstract static class SetRuidNode extends CoreMethodArrayArgumentsNode {

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public int setruid(int uid) {
            throw new RaiseException(coreExceptions().notImplementedError("setruid", this));
        }

    }

    @CoreMethod(names = "setuid", isModuleFunction = true, required = 1, lowerFixnum = 1, unsafe = UnsafeGroup.PROCESSES)
    public abstract static class SetUidNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int setuid(int uid) {
            return posix().setuid(uid);
        }

    }

    @CoreMethod(names = "setsid", isModuleFunction = true, unsafe = UnsafeGroup.PROCESSES)
    public abstract static class SetSidNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int setsid() {
            return posix().setsid();
        }

    }

    @CoreMethod(names = "flock", isModuleFunction = true, required = 2, lowerFixnum = { 1, 2 }, unsafe = UnsafeGroup.IO)
    public abstract static class FlockNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int flock(int fd, int constant) {
            return posix().flock(fd, constant);
        }

    }

    @CoreMethod(names = "major", isModuleFunction = true, required = 1, unsafe = UnsafeGroup.IO)
    public abstract static class MajorNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int major(long dev) {
            if (Platform.getPlatform().getOS() == OS.SOLARIS) {
                return (int) (dev >> 32); // Solaris has major number in the upper 32 bits.
            } else {
                return (int) ((dev >> 24) & 0xff);

            }
        }
    }

    @CoreMethod(names = "minor", isModuleFunction = true, required = 1, unsafe = UnsafeGroup.IO)
    public abstract static class MinorNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int minor(long dev) {
            if (Platform.getPlatform().getOS() == OS.SOLARIS) {
                return (int) dev; // Solaris has minor number in the lower 32 bits.
            } else {
                return (int) (dev & 0xffffff);
            }
        }

    }

    @CoreMethod(names = "rename", isModuleFunction = true, required = 2, unsafe = UnsafeGroup.IO)
    public abstract static class RenameNode extends CoreMethodArrayArgumentsNode {

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = {"isRubyString(path)", "isRubyString(other)"})
        public int rename(DynamicObject path, DynamicObject other) {
            return posix().rename(decodeUTF8(path), decodeUTF8(other));
        }

    }

    @CoreMethod(names = "rmdir", isModuleFunction = true, required = 1, unsafe = UnsafeGroup.IO)
    public abstract static class RmdirNode extends CoreMethodArrayArgumentsNode {

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = "isRubyString(path)")
        public int rmdir(DynamicObject path) {
            return posix().rmdir(path.toString());
        }

    }

    @CoreMethod(names = "getcwd", isModuleFunction = true, unsafe = UnsafeGroup.IO)
    public abstract static class GetcwdNode extends CoreMethodArrayArgumentsNode {

        @Child private RopeNodes.MakeLeafRopeNode makeLeafRopeNode;

        public GetcwdNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            makeLeafRopeNode = RopeNodesFactory.MakeLeafRopeNodeGen.create(null, null, null, null);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject getcwd() {
            final String cwd = posix().getcwd();
            final String path = getContext().getCurrentDirectory();
            assert path.equals(cwd);

            // TODO (nirvdrum 12-Sept-16) The rope table always returns UTF-8, but this call should be based on Encoding.default_external and reflect updates to that value.
            return StringOperations.createString(getContext(), getContext().getRopeTable().getRope(path));
        }

    }

    @CoreMethod(names = "errno", isModuleFunction = true, unsafe = UnsafeGroup.PROCESSES)
    public abstract static class ErrnoNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int errno() {
            return posix().errno();
        }

    }

    @CoreMethod(names = "errno=", isModuleFunction = true, required = 1, lowerFixnum = 1, unsafe = UnsafeGroup.PROCESSES)
    public abstract static class ErrnoAssignNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int errno(int errno) {
            posix().errno(errno);
            return 0;
        }

    }

    @CoreMethod(names = "fcntl", isModuleFunction = true, required = 3, lowerFixnum = {1, 2, 3}, unsafe = UnsafeGroup.IO)
    public abstract static class FcntlNode extends CoreMethodArrayArgumentsNode {

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = "isNil(nil)")
        public int fcntl(int fd, int fcntl, Object nil) {
            return posix().fcntl(fd, Fcntl.valueOf(fcntl));
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public int fcntl(int fd, int fcntl, int arg) {
            return posix().fcntlInt(fd, Fcntl.valueOf(fcntl), arg);
        }

    }

    @CoreMethod(names = "getpgid", isModuleFunction = true, required = 1, lowerFixnum = 1, unsafe = UnsafeGroup.PROCESSES)
    public abstract static class GetpgidNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int getpgid(int pid) {
            return posix().getpgid(pid);
        }

    }

    @CoreMethod(names = "getpgrp", isModuleFunction = true, unsafe = UnsafeGroup.PROCESSES)
    public abstract static class GetpgrpNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int getpgrp() {
            return posix().getpgrp();
        }

    }

    @CoreMethod(names = "isatty", isModuleFunction = true, required = 1, lowerFixnum = 1, unsafe = UnsafeGroup.IO)
    public abstract static class IsATTYNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int isATTY(int fd) {
            return posix().isatty(fd);
        }

    }

    @CoreMethod(names = "getppid", isModuleFunction = true, unsafe = UnsafeGroup.PROCESSES)
    public abstract static class GetppidNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int getppid() {
            return posix().getppid();
        }

    }

    @CoreMethod(names = "send", isModuleFunction = true, required = 4, lowerFixnum = {1, 3, 4}, unsafe = UnsafeGroup.IO)
    public abstract static class SendNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = "isRubyPointer(buffer)")
        public int send(int descriptor, DynamicObject buffer, int bytes, int flags) {
            return nativeSockets().send(descriptor, Layouts.POINTER.getPointer(buffer), bytes, flags);
        }

    }

    @CoreMethod(names = "symlink", isModuleFunction = true, required = 2, unsafe = UnsafeGroup.IO)
    public abstract static class SymlinkNode extends CoreMethodArrayArgumentsNode {

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = {"isRubyString(first)", "isRubyString(second)"})
        public int symlink(DynamicObject first, DynamicObject second) {
            return posix().symlink(first.toString(), second.toString());
        }

    }

    @CoreMethod(names = "_getaddrinfo", isModuleFunction = true, required = 4, unsafe = UnsafeGroup.IO)
    public abstract static class GetAddrInfoNode extends CoreMethodArrayArgumentsNode {

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = {"isNil(hostName)", "isRubyString(serviceName)"})
        public int getaddrinfoNil(DynamicObject hostName, DynamicObject serviceName, DynamicObject hintsPointer, DynamicObject resultsPointer) {
            return getaddrinfoString(create7BitString("0.0.0.0", UTF8Encoding.INSTANCE), serviceName, hintsPointer, resultsPointer);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = {"isRubyString(hostName)", "isRubyString(serviceName)", "isRubyPointer(hintsPointer)", "isRubyPointer(resultsPointer)"})
        public int getaddrinfoString(DynamicObject hostName, DynamicObject serviceName, DynamicObject hintsPointer, DynamicObject resultsPointer) {
            return nativeSockets().getaddrinfo(
                    StringOperations.rope(hostName).toString(),
                    StringOperations.rope(serviceName).toString(),
                    Layouts.POINTER.getPointer(hintsPointer),
                    Layouts.POINTER.getPointer(resultsPointer));
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = {"isRubyString(hostName)", "isNil(serviceName)", "isRubyPointer(hintsPointer)", "isRubyPointer(resultsPointer)"})
        public int getaddrinfo(DynamicObject hostName, DynamicObject serviceName, DynamicObject hintsPointer, DynamicObject resultsPointer) {
            return nativeSockets().getaddrinfo(
                    StringOperations.rope(hostName).toString(),
                    null,
                    Layouts.POINTER.getPointer(hintsPointer),
                    Layouts.POINTER.getPointer(resultsPointer));
        }

    }

    @CoreMethod(names = "_connect", isModuleFunction = true, required = 3, lowerFixnum = { 1, 3 }, unsafe = UnsafeGroup.IO)
    public abstract static class ConnectNode extends CoreMethodArrayArgumentsNode {

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = "isRubyPointer(address)")
        public int connect(int socket, DynamicObject address, int address_len) {
            return nativeSockets().connect(socket, Layouts.POINTER.getPointer(address), address_len);
        }

    }

    @CoreMethod(names = "freeaddrinfo", isModuleFunction = true, required = 1, unsafe = UnsafeGroup.IO)
    public abstract static class FreeAddrInfoNode extends CoreMethodArrayArgumentsNode {

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = "isRubyPointer(addrInfo)")
        public DynamicObject freeaddrinfo(DynamicObject addrInfo) {
            nativeSockets().freeaddrinfo(Layouts.POINTER.getPointer(addrInfo));
            return nil();
        }

    }

    @CoreMethod(names = "_getnameinfo", isModuleFunction = true, required = 7, lowerFixnum = {2, 4, 6, 7}, unsafe = UnsafeGroup.IO)
    public abstract static class GetNameInfoNode extends CoreMethodArrayArgumentsNode {

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = {"isRubyPointer(sa)", "isRubyPointer(host)", "isRubyPointer(serv)"})
        public int getnameinfo(DynamicObject sa, int salen, DynamicObject host, int hostlen, DynamicObject serv, int servlen, int flags) {
            assert hostlen > 0;
            assert servlen > 0;

            return nativeSockets().getnameinfo(
                    Layouts.POINTER.getPointer(sa),
                    salen,
                    Layouts.POINTER.getPointer(host),
                    hostlen,
                    Layouts.POINTER.getPointer(serv),
                    servlen,
                    flags);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = {"isRubyPointer(sa)", "isNil(host)", "isRubyPointer(serv)"})
        public int getnameinfoNullHost(DynamicObject sa, int salen, DynamicObject host, int hostlen, DynamicObject serv, int servlen, int flags) {
            assert hostlen == 0;
            assert servlen > 0;

            return nativeSockets().getnameinfo(
                    Layouts.POINTER.getPointer(sa),
                    salen,
                    PointerPrimitiveNodes.NULL_POINTER,
                    hostlen,
                    Layouts.POINTER.getPointer(serv),
                    servlen,
                    flags);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = {"isRubyPointer(sa)", "isRubyPointer(host)", "isNil(serv)"})
        public int getnameinfoNullService(DynamicObject sa, int salen, DynamicObject host, int hostlen, DynamicObject serv, int servlen, int flags) {
            assert hostlen > 0;
            assert servlen == 0;

            return nativeSockets().getnameinfo(
                    Layouts.POINTER.getPointer(sa),
                    salen,
                    Layouts.POINTER.getPointer(host),
                    hostlen,
                    PointerPrimitiveNodes.NULL_POINTER,
                    servlen,
                    flags);
        }

    }

    @CoreMethod(names = "shutdown", isModuleFunction = true, required = 2, lowerFixnum = {1, 2}, unsafe = UnsafeGroup.IO)
    public abstract static class ShutdownNode extends CoreMethodArrayArgumentsNode {

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public int shutdown(int socket, int how) {
            return nativeSockets().shutdown(socket, how);
        }

    }

    @CoreMethod(names = "socket", isModuleFunction = true, required = 3, lowerFixnum = {1, 2, 3}, unsafe = UnsafeGroup.IO)
    public abstract static class SocketNode extends CoreMethodArrayArgumentsNode {

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public int getnameinfo(int domain, int type, int protocol) {
            return nativeSockets().socket(domain, type, protocol);
        }

    }

    @CoreMethod(names = "setsockopt", isModuleFunction = true, required = 5, lowerFixnum = { 1, 2, 3, 5 }, unsafe = UnsafeGroup.IO)
    public abstract static class SetSockOptNode extends CoreMethodArrayArgumentsNode {

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = "isRubyPointer(optionValue)")
        public int setsockopt(int socket, int level, int optionName, DynamicObject optionValue, int optionLength) {
            return nativeSockets().setsockopt(socket, level, optionName, Layouts.POINTER.getPointer(optionValue), optionLength);
        }

    }

    @CoreMethod(names = "_bind", isModuleFunction = true, required = 3, lowerFixnum = {1, 3}, unsafe = UnsafeGroup.IO)
    public abstract static class BindNode extends CoreMethodArrayArgumentsNode {

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = "isRubyPointer(address)")
        public int bind(int socket, DynamicObject address, int addressLength) {
            return nativeSockets().bind(socket, Layouts.POINTER.getPointer(address), addressLength);
        }

    }

    @CoreMethod(names = "listen", isModuleFunction = true, required = 2, lowerFixnum = {1, 2}, unsafe = UnsafeGroup.IO)
    public abstract static class ListenNode extends CoreMethodArrayArgumentsNode {

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public int listen(int socket, int backlog) {
            return nativeSockets().listen(socket, backlog);
        }

    }

    @CoreMethod(names = "gethostname", isModuleFunction = true, required = 2, lowerFixnum = 2, unsafe = UnsafeGroup.IO)
    public abstract static class GetHostNameNode extends CoreMethodArrayArgumentsNode {

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = "isRubyPointer(name)")
        public int getHostName(DynamicObject name, int nameLength) {
            return nativeSockets().gethostname(Layouts.POINTER.getPointer(name), nameLength);
        }

    }

    @CoreMethod(names = "_getpeername", isModuleFunction = true, required = 3, lowerFixnum = 1, unsafe = UnsafeGroup.IO)
    public abstract static class GetPeerNameNode extends CoreMethodArrayArgumentsNode {

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = {"isRubyPointer(address)", "isRubyPointer(addressLength)"})
        public int getPeerName(int socket, DynamicObject address, DynamicObject addressLength) {
            return nativeSockets().getpeername(socket, Layouts.POINTER.getPointer(address), Layouts.POINTER.getPointer(addressLength));
        }

    }

    @CoreMethod(names = "_getsockname", isModuleFunction = true, required = 3, lowerFixnum = 1, unsafe = UnsafeGroup.IO)
    public abstract static class GetSockNameNode extends CoreMethodArrayArgumentsNode {

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = {"isRubyPointer(address)", "isRubyPointer(addressLength)"})
        public int getSockName(int socket, DynamicObject address, DynamicObject addressLength) {
            return nativeSockets().getsockname(socket, Layouts.POINTER.getPointer(address), Layouts.POINTER.getPointer(addressLength));
        }

    }

    @CoreMethod(names = "_getsockopt", isModuleFunction = true, required = 5, lowerFixnum = {1, 2, 3}, unsafe = UnsafeGroup.IO)
    public abstract static class GetSockOptNode extends CoreMethodArrayArgumentsNode {

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = { "isRubyPointer(optval)", "isRubyPointer(optlen)" })
        public int getSockOptions(int sockfd, int level, int optname, DynamicObject optval, DynamicObject optlen) {
            return nativeSockets().getsockopt(sockfd, level, optname, Layouts.POINTER.getPointer(optval), Layouts.POINTER.getPointer(optlen));
        }

        // This should probably done at a higher-level, but rubysl/socket does not handle it.
        @Specialization(guards = { "isRubySymbol(level)", "isRubySymbol(optname)", "isRubyPointer(optval)", "isRubyPointer(optlen)" })
        public int getSockOptionsSymbols(
                VirtualFrame frame,
                int sockfd,
                DynamicObject level,
                DynamicObject optname,
                DynamicObject optval,
                DynamicObject optlen,
                @Cached("new()") SnippetNode snippetNode) {
            int levelInt = (int) snippetNode.execute(frame, "Socket.const_get('SOL_' + name)", "name", Layouts.SYMBOL.getString(level));
            int optnameInt = (int) snippetNode.execute(frame, "Socket.const_get('SOL_' + name)", "name", Layouts.SYMBOL.getString(optname));
            return getSockOptions(sockfd, levelInt, optnameInt, optval, optlen);
        }

    }

    @CoreMethod(names = "close", isModuleFunction = true, required = 1, lowerFixnum = 1, unsafe = UnsafeGroup.IO)
    public abstract static class CloseNode extends CoreMethodArrayArgumentsNode {

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public int close(int file) {
            return posix().close(file);
        }

    }

    @CoreMethod(names = "kill", isModuleFunction = true, unsafe = { UnsafeGroup.PROCESSES, UnsafeGroup.SIGNALS }, required = 3)
    public abstract static class KillNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = "isRubyString(signalName)")
        public int kill(int pid, int signalNumber, DynamicObject signalName) {
            int self = posix().getpid();

            if (self == pid) {
                Signal signal = getContext().getNativePlatform().getSignalManager().createSignal(StringOperations.getString(signalName));
                return raise(signal);
            } else {
                return posix().kill(pid, signalNumber);
            }
        }

        @TruffleBoundary
        private int raise(Signal signal) {
            try {
                getContext().getNativePlatform().getSignalManager().raise(signal);
            } catch (IllegalArgumentException e) {
                throw new RaiseException(coreExceptions().argumentError(e.getMessage(), this));
            }
            return 1;
        }

    }

}
