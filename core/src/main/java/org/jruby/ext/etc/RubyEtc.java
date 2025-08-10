package org.jruby.ext.etc;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;
import jnr.constants.Constant;
import jnr.constants.ConstantSet;
import jnr.constants.platform.Errno;
import jnr.constants.platform.Sysconf;
import jnr.constants.platform.Confstr;
import jnr.constants.platform.Pathconf;

import org.jruby.RubyArray;
import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.exceptions.RaiseException;
import jnr.posix.Passwd;
import jnr.posix.Group;
import jnr.posix.POSIX;
import jnr.posix.util.Platform;
import org.jruby.Ruby;
import org.jruby.RubyIO;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.RubyStruct;
import org.jruby.ext.rbconfig.RbConfigLibrary;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.SafePropertyAccessor;
import org.jruby.util.io.OpenFile;
import java.nio.ByteBuffer;

import static org.jruby.api.Access.*;
import static org.jruby.api.Convert.*;
import static org.jruby.api.Create.*;
import static org.jruby.api.Define.defineModule;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.notImplementedError;
import static org.jruby.api.Error.runtimeError;
import static org.jruby.api.Warn.warn;

@JRubyModule(name="Etc")
public class RubyEtc {
    public static class IOExt {
        @JRubyMethod
        public static synchronized IRubyObject pathconf(ThreadContext context, IRubyObject recv, IRubyObject arg) {
            Pathconf name = Pathconf.valueOf(toLong(context, arg));
            RubyIO io = (RubyIO) recv;
            OpenFile fptr = io.getOpenFileChecked();
            POSIX posix = context.runtime.getPosix();
            long ret = posix.fpathconf(fptr.getFileno(), name);
            if (ret == -1) {
                if (posix.errno() == 0) {
                    return context.nil;
                } else if (posix.errno() == Errno.EOPNOTSUPP.intValue()) {
                    throw context.runtime.newNotImplementedError("pathconf() function is unimplemented on this machine");
                } else {
                    throw context.runtime.newErrnoFromLastPOSIXErrno();
                }
            }
            return asFixnum(context, ret);
        }
    }
    
    public static RubyModule createEtcModule(ThreadContext context) {
        RubyModule Etc = defineModule(context, "Etc").defineMethods(context, RubyEtc.class);

        context.runtime.setEtc(Etc);
        ioClass(context).defineMethods(context, IOExt.class);

        if (!Platform.IS_WINDOWS) {
            for (Constant c : ConstantSet.getConstantSet("Sysconf")) {
                String name = c.name().substring(1); // leading "_"
                Etc.defineConstant(context, name, asFixnum(context, c.intValue()));
            }
            for (Constant c : ConstantSet.getConstantSet("Confstr")) {
                String name = c.name().substring(1); // leading "_"
                Etc.defineConstant(context, name, asFixnum(context, c.intValue()));
            }
            for (Constant c : ConstantSet.getConstantSet("Pathconf")) {
                String name = c.name().substring(1); // leading "_"
                Etc.defineConstant(context, name, asFixnum(context, c.intValue()));
            }
        }
        
        definePasswdStruct(context, Etc);
        defineGroupStruct(context, Etc);
        
        return Etc;
    }
    
    private static void definePasswdStruct(ThreadContext context, RubyModule Etc) {
        IRubyObject[] args = new IRubyObject[] {
                context.nil,
                asSymbol(context, "name"),
                asSymbol(context, "passwd"),
                asSymbol(context, "uid"),
                asSymbol(context, "gid"),
                asSymbol(context, "gecos"),
                asSymbol(context, "dir"),
                asSymbol(context, "shell"),
                asSymbol(context, "change"),
                asSymbol(context, "uclass"),
                asSymbol(context, "expire")
        };

        var PasswdStruct = RubyStruct.newInstance(context, structClass(context), args, Block.NULL_BLOCK);
        context.runtime.setPasswdStruct(PasswdStruct);
        Etc.defineConstant(context, "Passwd", PasswdStruct);
    }

    private static void defineGroupStruct(ThreadContext context, RubyModule Etc) {
        IRubyObject[] args = new IRubyObject[] {
                context.nil,
                asSymbol(context, "name"),
                asSymbol(context, "passwd"),
                asSymbol(context, "gid"),
                asSymbol(context, "mem")
        };

        var GroupStruct = RubyStruct.newInstance(context, structClass(context), args, Block.NULL_BLOCK);
        context.runtime.setGroupStruct(GroupStruct);
        Etc.defineConstant(context, "Group", GroupStruct);
    }
    
    private static IRubyObject setupPasswd(ThreadContext context, Passwd passwd) {
        IRubyObject[] args = new IRubyObject[] {
                newString(context, passwd.getLoginName()),
                newString(context, passwd.getPassword()),
                asFixnum(context, passwd.getUID()),
                asFixnum(context, passwd.getGID()),
                newString(context, passwd.getGECOS()),
                newString(context, passwd.getHome()),
                newString(context, passwd.getShell()),
                asFixnum(context, passwd.getPasswdChangeTime()),
                newString(context, passwd.getAccessClass()),
                asFixnum(context, passwd.getExpire())

        };
        
        return newStruct(context, (RubyClass) context.runtime.getPasswdStruct(), args, Block.NULL_BLOCK);
    }

    
    private static IRubyObject setupGroup(ThreadContext context, Group group) {
        IRubyObject[] args = new IRubyObject[] {
                newString(context, group.getName()),
                newString(context, group.getPassword()),
                asFixnum(context, group.getGID()),
                intoStringArray(context, group.getMembers())
        };
        
        return newStruct(context, (RubyClass) context.runtime.getGroupStruct(), args, Block.NULL_BLOCK);
    }

    private static IRubyObject intoStringArray(ThreadContext context, String[] members) {
        IRubyObject[] arr = new IRubyObject[members.length];
        for(int i = 0; i<arr.length; i++) {
            arr[i] = newString(context, members[i]);
        }
        return RubyArray.newArrayMayCopy(context.runtime, arr);
    }
    
    @JRubyMethod(module = true)
    public static synchronized IRubyObject sysconf(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        Sysconf name = Sysconf.valueOf(toLong(context, arg));
        POSIX posix = context.runtime.getPosix();
        posix.errno(0);
        long ret = posix.sysconf(name);

        if (ret == -1) {
            int errno = posix.errno();

            if (errno == Errno.ENOENT.intValue() || errno == 0) {
                return context.nil;
            } else if (errno == Errno.EOPNOTSUPP.intValue()) {
                throw notImplementedError(context, "sysconf() function is unimplemented on this machine");
            } else {
                throw context.runtime.newErrnoFromLastPOSIXErrno();
            }
        }
        return asFixnum(context, ret);
    }
    
    @JRubyMethod(module = true)
    public static synchronized IRubyObject confstr(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        Confstr name = Confstr.valueOf(toLong(context, arg));
        ByteBuffer buf;

        POSIX posix = context.runtime.getPosix();
        int n = posix.confstr(name, null, 0);
        int ret = -1;

        if (n > 0) {
            buf = ByteBuffer.allocate(n);
            ret = posix.confstr(name, buf, n);
        } else {
            buf = ByteBuffer.allocate(0);
        }
        
        if (ret == -1) {
            if (posix.errno() == 0) {
                return context.nil;
            } else if (posix.errno() == Errno.EOPNOTSUPP.intValue()) {
                throw context.runtime.newNotImplementedError("confstr() function is unimplemented on this machine");
            } else {
                throw context.runtime.newErrnoFromLastPOSIXErrno();
            }
        }

        buf.flip();
        return newString(context, new ByteList(buf.array(), 0, n - 1));
    }

    @Deprecated
    public static synchronized IRubyObject getpwuid(IRubyObject recv, IRubyObject[] args) {
        return getpwuid(((RubyBasicObject) recv).getCurrentContext(), recv, args);
    }

    @JRubyMethod(optional = 1, checkArity = false, module = true)
    public static synchronized IRubyObject getpwuid(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 0, 1);

        POSIX posix = context.runtime.getPosix();
        IRubyObject oldExc = context.getErrorInfo(); // Save $!
        try {
            int uid = args.length == 0 ? posix.getuid() : toInt(context, args[0]);
            Passwd pwd = posix.getpwuid(uid);
            if (pwd == null) {
                if (Platform.IS_WINDOWS) return context.nil;

                throw argumentError(context, "can't find user for " + uid);
            }
            return setupPasswd(context, pwd);
        } catch (RaiseException re) {
            if (context.runtime.getNotImplementedError().isInstance(re.getException())) {
                context.setErrorInfo(oldExc); // Restore $!
                return context.nil;
            }
            throw re;
        } catch (Exception e) {
            if (context.runtime.getDebug().isTrue()) warn(context, "Etc.getpwuid is not supported by JRuby on this platform");

            return context.nil;
        }
    }

    @Deprecated(since = "10.0")
    public static synchronized IRubyObject getpwnam(IRubyObject recv, IRubyObject name) {
        return getpwnam(((RubyBasicObject) recv).getCurrentContext(), recv, name);
    }

    @JRubyMethod(module = true)
    public static synchronized IRubyObject getpwnam(ThreadContext context, IRubyObject recv, IRubyObject name) {
        String nam = name.convertToString().toString();
        try {
            Passwd pwd = context.runtime.getPosix().getpwnam(nam);
            if (pwd == null) {
                if (Platform.IS_WINDOWS) return context.nil;

                throw argumentError(context, "can't find user for " + nam);
            }
            return setupPasswd(context, pwd);
        } catch (RaiseException e) {
            throw e;
        } catch (Exception e) {
            if (context.runtime.getDebug().isTrue()) {
                warn(context, "Etc.getpwnam is not supported by JRuby on this platform");
            }
            return context.nil;
        }
    }

    @Deprecated(since = "10.0")
    public static synchronized IRubyObject passwd(IRubyObject recv, Block block) {
        return passwd(((RubyBasicObject) recv).getCurrentContext(), recv, block);
    }

    @JRubyMethod(module = true)
    public static synchronized IRubyObject passwd(ThreadContext context, IRubyObject recv, Block block) {
        var posix = context.runtime.getPosix();
        try {
            posix.getpwent(); // call getpwent to fail early if unsupported
            if (block.isGiven()) {
                if (!iteratingPasswd.compareAndSet(false, true)) throw runtimeError(context, "parallel passwd iteration");

                posix.setpwent();
                try {
                    Passwd pw;
                    while((pw = posix.getpwent()) != null) {
                        block.yield(context, setupPasswd(context, pw));
                    }
                } finally {
                    posix.endpwent();
                    iteratingPasswd.set(false);
                }
            }

            Passwd pw = posix.getpwent();
            return pw != null ? setupPasswd(context, pw) : context.nil;
        } catch (Exception e) {
            if (context.runtime.getDebug().isTrue()) {
                warn(context, "Etc.passwd is not supported by JRuby on this platform");
            }
            return context.nil;
        }
    }

    @Deprecated(since = "10.0")
    public static synchronized IRubyObject getlogin(IRubyObject recv) {
        return getlogin(((RubyBasicObject) recv).getCurrentContext(), recv);
    }

    @JRubyMethod(module = true)
    public static synchronized IRubyObject getlogin(ThreadContext context, IRubyObject recv) {
        try {
            String login = context.runtime.getPosix().getlogin();
            if (login != null) return newString(context, login);

            login = System.getenv("USER");
            if (login != null) return newString(context, login);

            return context.nil;
        } catch (Exception e) {
            // fall back on env entry for USER
            return newString(context, System.getProperty("user.name"));
        }
    }

    @Deprecated(since = "10.0")
    public static synchronized IRubyObject endpwent(IRubyObject recv) {
        return endpwent(((RubyBasicObject) recv).getCurrentContext(), recv);
    }

    @JRubyMethod(module = true)
    public static synchronized IRubyObject endpwent(ThreadContext context, IRubyObject recv) {
        try {
            context.runtime.getPosix().endpwent();
        } catch (Exception e) {
            if (context.runtime.getDebug().isTrue()) {
                warn(context, "Etc.endpwent is not supported by JRuby on this platform");
            }
        }
        return context.nil;
    }

    @Deprecated(since = "10.0")
    public static synchronized IRubyObject setpwent(IRubyObject recv) {
        return setpwent(((RubyBasicObject) recv).getCurrentContext(), recv);
    }

    @JRubyMethod(module = true)
    public static synchronized IRubyObject setpwent(ThreadContext context, IRubyObject recv) {
        try {
            context.runtime.getPosix().setpwent();
        } catch (Exception e) {
            if (context.runtime.getDebug().isTrue()) {
                warn(context, "Etc.setpwent is not supported by JRuby on this platform");
            }
        }
        return context.nil;
    }

    @Deprecated(since = "10.0")
    public static synchronized IRubyObject getpwent(IRubyObject recv) {
        return getpwent(((RubyBasicObject) recv).getCurrentContext(), recv);
    }

    @JRubyMethod(module = true)
    public static synchronized IRubyObject getpwent(ThreadContext context, IRubyObject recv) {
        try {
            Passwd passwd = context.runtime.getPosix().getpwent();

            return passwd != null ? setupPasswd(context, passwd) : context.nil;
        } catch (Exception e) {
            if (context.runtime.getDebug().isTrue()) {
                warn(context, "Etc.getpwent is not supported by JRuby on this platform");
            }
            return context.nil;
        }
    }

    @Deprecated(since = "10.0")
    public static synchronized IRubyObject getgrnam(IRubyObject recv, IRubyObject name) {
        return getgrnam(((RubyBasicObject) recv).getCurrentContext(), recv, name);
    }

    @JRubyMethod(module = true)
    public static synchronized IRubyObject getgrnam(ThreadContext context, IRubyObject recv, IRubyObject name) {
        String nam = name.convertToString().toString();
        try {
            Group grp = context.runtime.getPosix().getgrnam(nam);
            if (grp == null) {
                if (Platform.IS_WINDOWS) return context.nil;
                throw argumentError(context, "can't find group for " + nam);
            }
            return setupGroup(context, grp);
        } catch (RaiseException e) {
            throw e;
        } catch (Exception e) {
            if (context.runtime.getDebug().isTrue()) {
                warn(context, "Etc.getgrnam is not supported by JRuby on this platform");
            }
            return context.nil;
        }
    }

    @Deprecated
    public static synchronized IRubyObject getgrgid(IRubyObject recv, IRubyObject[] args) {
        return getgrgid(((RubyBasicObject) recv).getCurrentContext(), recv, args);
    }

    @JRubyMethod(optional = 1, checkArity = false, module = true)
    public static synchronized IRubyObject getgrgid(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 0, 1);
        POSIX posix = context.runtime.getPosix();

        try {
            int gid = argc == 0 ? posix.getgid() : toInt(context, args[0]);
            Group gr = posix.getgrgid(gid);
            if(gr == null) {
                if (Platform.IS_WINDOWS) return context.nil;
                throw argumentError(context, "can't find group for " + gid);
            }
            return setupGroup(context, gr);
        } catch (RaiseException re) {
            throw re;
        } catch (Exception e) {
            if (context.runtime.getDebug().isTrue()) {
                warn(context, "Etc.getgrgid is not supported by JRuby on this platform");
            }
            return context.nil;
        }
    }

    @Deprecated(since = "10.0")
    public static synchronized IRubyObject endgrent(IRubyObject recv) {
        return endgrent(((RubyBasicObject) recv).getCurrentContext(), recv);
    }

    @JRubyMethod(module = true)
    public static synchronized IRubyObject endgrent(ThreadContext context, IRubyObject recv) {
        try {
            context.runtime.getPosix().endgrent();
        } catch (Exception e) {
            if (context.runtime.getDebug().isTrue()) {
                warn(context, "Etc.engrent is not supported by JRuby on this platform");
            }
        }
        return context.nil;
    }

    @Deprecated(since = "10.0")
    public static synchronized IRubyObject setgrent(IRubyObject recv) {
        return setgrent(((RubyBasicObject) recv).getCurrentContext(), recv);
    }

    @JRubyMethod(module = true)
    public static synchronized IRubyObject setgrent(ThreadContext context, IRubyObject recv) {
        try {
            context.runtime.getPosix().setgrent();
        } catch (Exception e) {
            if (context.runtime.getDebug().isTrue()) {
                warn(context, "Etc.setgrent is not supported by JRuby on this platform");
            }
        }
        return context.nil;
    }

    @Deprecated(since = "10.0")
    public static synchronized IRubyObject group(IRubyObject recv, Block block) {
        return group(((RubyBasicObject) recv).getCurrentContext(), recv, block);
    }

    @JRubyMethod(module = true)
    public static synchronized IRubyObject group(ThreadContext context, IRubyObject recv, Block block) {
        POSIX posix = context.runtime.getPosix();

        try {
            // try to read grent to fail fast
            posix.getgrent();
        } catch (Exception e) {
            if (context.runtime.getDebug().isTrue()) {
                warn(context, "Etc.group is not supported by JRuby on this platform");
            }
        }

        if (block.isGiven()) {
            Boolean blocking = (Boolean)recv.getInternalVariables().getInternalVariable("group_blocking");
            if (blocking != null && blocking) throw runtimeError(context, "parallel group iteration");

            try {
                recv.getInternalVariables().setInternalVariable("group_blocking", true);

                posix.setgrent();
                Group gr;
                while((gr = posix.getgrent()) != null) {
                    block.yield(context, setupGroup(context, gr));
                }
            } finally {
                posix.endgrent();
                recv.getInternalVariables().setInternalVariable("group_blocking", false);
            }
        } else {
            Group gr = posix.getgrent();
            return gr != null ? setupGroup(context, gr) : context.nil;
        }

        return context.nil;
    }

    @Deprecated(since = "10.0")
    public static synchronized IRubyObject getgrent(IRubyObject recv) {
        return getgrent(((RubyBasicObject) recv).getCurrentContext(), recv);
    }

    @JRubyMethod(module = true)
    public static synchronized IRubyObject getgrent(ThreadContext context, IRubyObject recv) {
        try {
            Group gr;

            // We synchronize on this class so at least all JRuby instances in this classloader are safe.
            // See jruby/jruby#4057
            synchronized (RubyEtc.class) {
                gr = context.runtime.getPosix().getgrent();
            }

            return gr != null ? setupGroup(context, gr) : context.nil;
        } catch (Exception e) {
            if (context.runtime.getDebug().isTrue()) {
                warn(context, "Etc.getgrent is not supported by JRuby on this platform");
            }
            return context.nil;
        }
    }
    
    @JRubyMethod(module = true)
    public static synchronized IRubyObject systmpdir(ThreadContext context, IRubyObject recv) {
        ByteList tmp = ByteList.create(System.getProperty("java.io.tmpdir")); // default for all platforms except Windows
        if (Platform.IS_WINDOWS) {
            String commonAppData = System.getenv("CSIDL_COMMON_APPDATA");
            if (commonAppData != null) tmp = ByteList.create(commonAppData);
        }

        return newString(context, tmp, context.runtime.getDefaultExternalEncoding());
    }
    
    @JRubyMethod(module = true)
    public static synchronized IRubyObject sysconfdir(ThreadContext context, IRubyObject recv) {
        Ruby runtime = context.getRuntime();
        ByteList tmp = ByteList.create(RbConfigLibrary.getSysConfDir(runtime)); // default for all platforms except Windows
        
        if (Platform.IS_WINDOWS) {
            String localAppData = System.getenv("CSIDL_LOCAL_APPDATA");
            // TODO: need fallback mechanism
            if (localAppData != null) tmp = ByteList.create(localAppData);
        }
        RubyString ret = RubyString.newString(runtime, tmp, runtime.getDefaultExternalEncoding());

        return ret;
    }

    @JRubyMethod(module = true)
    public static synchronized IRubyObject nprocessors(ThreadContext context, IRubyObject recv) {
        int nprocs = Runtime.getRuntime().availableProcessors();
        return asFixnum(context, nprocs);
    }

    @JRubyMethod(module = true)
    public static synchronized IRubyObject uname(ThreadContext context, IRubyObject self) {
        RubyHash uname = newHash(context);

        uname.op_aset(context,
                asSymbol(context, "sysname"),
                newString(context, SafePropertyAccessor.getProperty("os.name", "unknown")));
        try {
            uname.op_aset(context,
                    asSymbol(context, "nodename"),
                    newString(context, InetAddress.getLocalHost().getHostName()));
        } catch (UnknownHostException uhe) {
            uname.op_aset(context,
                    asSymbol(context, "nodename"),
                    newString(context, "unknown"));
        }
        uname.put(asSymbol(context, "release"), newString(context, "unknown"));
        uname.put(asSymbol(context, "version"), newString(context, SafePropertyAccessor.getProperty("os.version")));
        uname.put(asSymbol(context, "machine"), newString(context, SafePropertyAccessor.getProperty("os.arch")));

        return uname;
    }
    
    private static final AtomicBoolean iteratingPasswd = new AtomicBoolean(false);
}
