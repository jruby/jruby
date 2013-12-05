/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2013 Benoit Daloze <eregontp@gmail.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ext.pathname;

import static org.jruby.anno.FrameField.BACKREF;

import org.jruby.CompatVersion;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

@JRubyClass(name = "Pathname")
public class RubyPathname extends RubyObject {
    private RubyString path;

    static void createPathnameClass(Ruby runtime) {
        RubyClass cPathname = runtime.defineClass("Pathname", runtime.getObject(),
                PATHNAME_ALLOCATOR);

        cPathname.defineAnnotatedMethods(RubyPathname.class);

        runtime.getKernel().defineAnnotatedMethods(PathnameKernelMethods.class);

        defineDelegateMethods(cPathname, runtime.getFile(), "atime", "ctime", "mtime", "ftype",
                "rename", "stat", "lstat", "truncate", "extname", "open");
        defineDelegateMethodsAppendPath(cPathname, runtime.getFile(), "chmod", "lchmod", "chown",
                "lchown", "utime");
        defineDelegateMethodsSinglePath(cPathname, runtime.getFile(), "realpath", "realdirpath",
                "basename", "dirname", "expand_path", "readlink");
        defineDelegateMethodsArrayOfPaths(cPathname, runtime.getFile(), "split");

        defineDelegateMethods(cPathname, runtime.getIO(), "read", "binread", "write", "binwrite",
                "readlines", "sysopen");

        defineDelegateMethods(cPathname, runtime.getFileTest(), "blockdev?", "chardev?",
                "executable?", "executable_real?", "exist?", "grpowned?", "directory?", "file?",
                "pipe?", "socket?", "owned?", "readable?", "world_readable?", "readable_real?",
                "setuid?", "setgid?", "size", "size?", "sticky?", "symlink?", "writable?",
                "world_writable?", "writable_real?", "zero?");

        defineDelegateMethods(cPathname, runtime.getDir(), "mkdir", "rmdir");
        defineDelegateMethodsArrayOfPaths(cPathname, runtime.getDir(), "entries");

        cPathname.undefineMethod("=~");
    }

    static interface ReturnValueMapper {
        IRubyObject map(ThreadContext context, RubyClass klazz, IRubyObject value);
    }

    static interface AddArg {
        IRubyObject[] addArg(IRubyObject[] args, RubyString path);
    }

    private static final ReturnValueMapper IDENTITY_MAPPER = new ReturnValueMapper() {
        @Override
        public IRubyObject map(ThreadContext context, RubyClass klazz, IRubyObject value) {
            return value;
        }
    };

    private static final ReturnValueMapper SINGLE_PATH_MAPPER = new ReturnValueMapper() {
        @Override
        public IRubyObject map(ThreadContext context, RubyClass klazz, IRubyObject value) {
            return newInstance(context, klazz, value);
        }
    };

    private static final ReturnValueMapper ARRAY_OF_PATHS_MAPPER = new ReturnValueMapper() {
        @Override
        public IRubyObject map(ThreadContext context, RubyClass klazz, IRubyObject value) {
            return mapToPathnames(context, klazz, value);
        }
    };

    private static final AddArg UNSHIFT_PATH = new AddArg() {
        @Override
        public IRubyObject[] addArg(IRubyObject[] args, RubyString path) {
            return insert(args, 0, path);
        }
    };

    private static final AddArg APPEND_PATH = new AddArg() {
        @Override
        public IRubyObject[] addArg(IRubyObject[] args, RubyString path) {
            return insert(args, args.length, path);
        }
    };

    private static void defineDelegateMethodsGeneric(RubyClass cPathname, final RubyModule klass,
            final ReturnValueMapper mapper, final AddArg addArg, String... methods) {
        for (String method : methods) {
            cPathname.addMethod(method, new JavaMethod.JavaMethodNBlock(cPathname,
                    Visibility.PUBLIC) {
                @Override
                public IRubyObject call(ThreadContext context, IRubyObject _self, RubyModule clazz,
                        String name, IRubyObject[] args, Block block) {
                    RubyPathname self = (RubyPathname) _self;
                    args = addArg.addArg(args, self.path);
                    return mapper.map(context, (RubyClass) clazz, klass.callMethod(context, name, args, block));
                }
            });
        }
    }

    private static void defineDelegateMethods(RubyClass cPathname, final RubyModule klass,
            String... methods) {
        defineDelegateMethodsGeneric(cPathname, klass, IDENTITY_MAPPER, UNSHIFT_PATH, methods);
    }

    private static void defineDelegateMethodsAppendPath(RubyClass cPathname,
            final RubyModule klass, String... methods) {
        defineDelegateMethodsGeneric(cPathname, klass, IDENTITY_MAPPER, APPEND_PATH, methods);
    }

    private static void defineDelegateMethodsSinglePath(RubyClass cPathname,
            final RubyModule klass, String... methods) {
        defineDelegateMethodsGeneric(cPathname, klass, SINGLE_PATH_MAPPER, UNSHIFT_PATH, methods);
    }

    private static void defineDelegateMethodsArrayOfPaths(RubyClass cPathname,
            final RubyModule klass, String... methods) {
        defineDelegateMethodsGeneric(cPathname, klass, ARRAY_OF_PATHS_MAPPER, UNSHIFT_PATH, methods);
    }

    public static class PathnameKernelMethods {
        @JRubyMethod(name = "Pathname", module = true, visibility = Visibility.PRIVATE)
        public static IRubyObject newPathname(IRubyObject recv, IRubyObject path) {
            return RubyPathname.newInstance(recv.getRuntime().getCurrentContext(), path);
        }
    }

    private static ObjectAllocator PATHNAME_ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyPathname(runtime, klass);
        }
    };

    public RubyPathname(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
        path = null;
    }

    public static RubyPathname newInstance(ThreadContext context, RubyClass klass, IRubyObject path) {
        RubyPathname pathname = new RubyPathname(context.runtime, klass);
        return (RubyPathname) pathname.initialize(context, path);
    }

    public static RubyPathname newInstance(ThreadContext context, IRubyObject path) {
        return newInstance(context, context.runtime.getClass("Pathname"), path);
    }

    @JRubyMethod
    public IRubyObject initialize(ThreadContext context, IRubyObject path) {
        String toPath = toPathMethod(context.runtime);
        if (path.respondsTo(toPath)) {
            path = path.callMethod(context, toPath);
        }

        RubyString str = path.convertToString();
        if (str.getByteList().indexOf('\0') != -1) {
            throw context.runtime.newArgumentError("pathname contains null byte");
        }

        infectBy(str);
        this.path = (RubyString) str.dup();
        // TODO: remove (either direct bridge to field or all native)
        setInstanceVariable("@path", this.path);
        return this;
    }

    private static String toPathMethod(Ruby runtime) {
        return runtime.is1_8() ? "to_str" : "to_path";
    }

    @JRubyMethod
    public IRubyObject initialize_copy(ThreadContext context, IRubyObject pathname) {
        super.initialize_copy(pathname);
        initialize(context, pathname);
        return this;
    }

    @JRubyMethod(compat = CompatVersion.RUBY1_8)
    public IRubyObject to_str(ThreadContext context) {
        return path;
    }

    @JRubyMethod(compat = CompatVersion.RUBY1_9)
    public IRubyObject to_path(ThreadContext context) {
        return path;
    }

    @Override
    @JRubyMethod
    public IRubyObject freeze(ThreadContext context) {
        path.freeze(context);
        return super.freeze(context);
    }

    @Override
    @JRubyMethod
    public IRubyObject taint(ThreadContext context) {
        path.taint(context);
        return super.taint(context);
    }

    @Override
    @JRubyMethod
    public IRubyObject untaint(ThreadContext context) {
        path.untaint(context);
        return super.untaint(context);
    }

    @Override
    @JRubyMethod(name = { "==", "eql?" })
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyPathname) {
            return Helpers.rbEqual(context, path, ((RubyPathname) other).path);
        } else {
            return context.runtime.getFalse();
        }
    }

    private int cmp(RubyPathname other) {
        byte[] a = path.getByteList().bytes();
        byte[] b = other.path.getByteList().bytes();
        int i;
        for (i = 0; i < a.length && i < b.length; i++) {
            byte ca = a[i];
            byte cb = b[i];
            if (ca == '/') {
                ca = '\0';
            }
            if (cb == '/') {
                cb = '\0';
            }
            if (ca != cb) {
                return ca < cb ? -1 : 1;
            }
        }
        if (i < a.length) {
            return 1;
        }
        if (i < b.length) {
            return -1;
        }
        return 0;
    }

    @Override
    @JRubyMethod(name = "<=>")
    public IRubyObject op_cmp(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyPathname) {
            return context.runtime.newFixnum(cmp((RubyPathname) other));
        } else {
            return context.nil;
        }
    }

    @JRubyMethod(name = "hash")
    public RubyFixnum hash(ThreadContext context) {
        return path.hash();
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @JRubyMethod
    public IRubyObject to_s(ThreadContext context) {
        return path.dup();
    }

    @JRubyMethod
    public IRubyObject inspect(ThreadContext context) {
        return context.runtime.newString("<Pathname:" + path + ">");
    }

    @JRubyMethod(required = 1, optional = 1, reads = BACKREF, writes = BACKREF)
    public IRubyObject sub(ThreadContext context, IRubyObject[] args, Block block) {
        IRubyObject result = path.callMethod(context, "sub", args, block);
        return newInstance(context, result);
    }

    @JRubyMethod
    public IRubyObject sub_ext(ThreadContext context, IRubyObject newExt) {
        IRubyObject ext = context.runtime.getFile().callMethod(context, "extname", path);
        IRubyObject newPath = path.chomp(context, ext).callMethod(context, "+", newExt);
        return newInstance(context, newPath);
    }

    /* Facade for File */

    @JRubyMethod(alias = "fnmatch?", required = 1, optional = 1)
    public IRubyObject fnmatch(ThreadContext context, IRubyObject[] args) {
        args = insertPath(args, 1);
        return context.runtime.getFile().callMethod(context, "fnmatch?", args);
    }

    @JRubyMethod
    public IRubyObject make_link(ThreadContext context, IRubyObject old) {
        IRubyObject[] args = new IRubyObject[] { old, path };
        return context.runtime.getFile().callMethod(context, "link", args);
    }

    @JRubyMethod
    public IRubyObject make_symlink(ThreadContext context, IRubyObject old) {
        IRubyObject[] args = new IRubyObject[] { old, path };
        return context.runtime.getFile().callMethod(context, "symlink", args);
    }

    /* Facade for IO */

    @JRubyMethod(optional = 3)
    public IRubyObject each_line(ThreadContext context, IRubyObject[] args, Block block) {
        return context.runtime.getIO().callMethod(context, "foreach", unshiftPath(args), block);
    }

    /* Facade for Dir */

    @JRubyMethod(alias = "pwd", meta = true)
    public static IRubyObject getwd(ThreadContext context, IRubyObject recv) {
        return newInstance(context, context.runtime.getDir().callMethod("getwd"));
    }

    @JRubyMethod(required = 1, optional = 1, meta = true)
    public static IRubyObject glob(ThreadContext context, IRubyObject recv, IRubyObject[] args,
            Block block) {
        // TODO: yield block while iterating
        RubyArray files = mapToPathnames(context, (RubyClass) recv,
                context.runtime.getDir().callMethod(context, "glob", args));
        if (block.isGiven()) {
            files.each(context, block);
            return context.nil;
        } else {
            return files;
        }
    }

    @JRubyMethod
    public IRubyObject opendir(ThreadContext context, Block block) {
        return context.runtime.getDir().callMethod(context, "open", new IRubyObject[] { path },
                block);
    }

    @JRubyMethod
    public IRubyObject each_entry(ThreadContext context, Block block) {
        if (block.isGiven()) {
            // TODO: yield block while iterating
            RubyArray entries = callMethod(context, "entries").convertToArray();
            entries.each(context, block);
            return context.nil;
        } else {
            return context.runtime.getDir().callMethod(context, "foreach");
        }
    }

    /* Mix of File and Dir */

    @JRubyMethod(name = {"unlink", "delete"})
    public IRubyObject unlink(ThreadContext context) {
        try {
            return context.runtime.getDir().callMethod(context, "unlink", path);
        } catch (RaiseException ex) {
            if (!context.runtime.getErrno().getClass("ENOTDIR").isInstance(ex.getException())) {
                throw ex;
            }
            return context.runtime.getFile().callMethod(context, "unlink", path);
        }
    }

    /* Helpers */

    private IRubyObject[] insertPath(IRubyObject[] args, int i) {
        return insert(args, i, path);
    }

    private IRubyObject[] unshiftPath(IRubyObject[] args) {
        return insert(args, 0, path);
    }

    private static IRubyObject[] insert(IRubyObject[] old, int i, IRubyObject obj) {
        IRubyObject[] ary = new IRubyObject[old.length + 1];
        if (i > 0) {
            System.arraycopy(old, 0, ary, 0, i);
        }
        ary[i] = obj;
        if (old.length > i) {
            System.arraycopy(old, i, ary, i + 1, old.length - i);
        }
        return ary;
    }

    private static RubyArray mapToPathnames(ThreadContext context, RubyClass clazz, IRubyObject ary) {
        RubyArray paths = ary.convertToArray();
        for (int i = 0; i < paths.size(); i++) {
            RubyString path = paths.eltOk(i).convertToString();
            paths.store(i, newInstance(context, clazz, path));
        }
        return paths;
    }
}
