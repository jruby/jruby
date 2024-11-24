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
import org.jruby.RubyHash;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.api.Convert;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.exceptions.RaiseException;
import jnr.posix.Passwd;
import jnr.posix.Group;
import jnr.posix.POSIX;
import jnr.posix.util.Platform;
import org.jruby.Ruby;
import org.jruby.RubyFixnum;
import org.jruby.RubyIO;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
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

import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.numericToLong;
import static org.jruby.api.Create.*;
import static org.jruby.api.Error.argumentError;

@JRubyModule(name="Etc")
public class RubyEtc {
    public static class IOExt {
        @JRubyMethod
        public static synchronized IRubyObject pathconf(ThreadContext context, IRubyObject recv, IRubyObject arg) {
            Pathconf name = Pathconf.valueOf(numericToLong(context, arg));
            RubyIO io = (RubyIO) recv;
            OpenFile fptr = io.getOpenFileChecked();
            POSIX posix = context.runtime.getPosix();
            long ret = posix.fpathconf(fptr.getFileno(), name);
            if (ret == -1) {
                if (posix.errno() == 0) {
                    return context.runtime.getNil();
                } else if (posix.errno() == Errno.EOPNOTSUPP.intValue()) {
                    throw context.runtime.newNotImplementedError("pathconf() function is unimplemented on this machine");
                } else {
                    throw context.runtime.newErrnoFromLastPOSIXErrno();
                }
            }
            return asFixnum(context, ret);
        }
    }
    
    public static RubyModule createEtcModule(Ruby runtime) {
        var context = runtime.getCurrentContext();
        RubyModule etcModule = runtime.defineModule("Etc");

        runtime.setEtc(etcModule);
        
        etcModule.defineAnnotatedMethods(RubyEtc.class);
        runtime.getIO().defineAnnotatedMethods(IOExt.class);

        if (!Platform.IS_WINDOWS) {
            for (Constant c : ConstantSet.getConstantSet("Sysconf")) {
                String name = c.name().substring(1); // leading "_"
                etcModule.setConstant(name, asFixnum(context, c.intValue()));
            }
            for (Constant c : ConstantSet.getConstantSet("Confstr")) {
                String name = c.name().substring(1); // leading "_"
                etcModule.setConstant(name, asFixnum(context, c.intValue()));
            }
            for (Constant c : ConstantSet.getConstantSet("Pathconf")) {
                String name = c.name().substring(1); // leading "_"
                etcModule.setConstant(name, asFixnum(context, c.intValue()));
            }
        }
        
        definePasswdStruct(runtime);
        defineGroupStruct(runtime);
        
        return etcModule;
    }
    
    private static void definePasswdStruct(Ruby runtime) {
        IRubyObject[] args = new IRubyObject[] {
                runtime.getNil(),
                runtime.newSymbol("name"),
                runtime.newSymbol("passwd"),
                runtime.newSymbol("uid"),
                runtime.newSymbol("gid"),
                runtime.newSymbol("gecos"),
                runtime.newSymbol("dir"),
                runtime.newSymbol("shell"),
                runtime.newSymbol("change"),
                runtime.newSymbol("uclass"),
                runtime.newSymbol("expire")
        };
        
        runtime.setPasswdStruct(RubyStruct.newInstance(runtime.getCurrentContext(), runtime.getStructClass(), args, Block.NULL_BLOCK));
        runtime.getEtc().defineConstant("Passwd", runtime.getPasswdStruct());
    }

    private static void defineGroupStruct(Ruby runtime) {
        IRubyObject[] args = new IRubyObject[] {
                runtime.getNil(),
                runtime.newSymbol("name"),
                runtime.newSymbol("passwd"),
                runtime.newSymbol("gid"),
                runtime.newSymbol("mem")
        };
        
        runtime.setGroupStruct(RubyStruct.newInstance(runtime.getCurrentContext(), runtime.getStructClass(), args, Block.NULL_BLOCK));
        runtime.getEtc().defineConstant("Group", runtime.getGroupStruct());
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
        
        return RubyStruct.newStruct(context.runtime.getPasswdStruct(), args, Block.NULL_BLOCK);
    }

    
    private static IRubyObject setupGroup(Ruby runtime, Group group) {
        IRubyObject[] args = new IRubyObject[] {
                runtime.newString(group.getName()),
                runtime.newString(group.getPassword()),
                runtime.newFixnum(group.getGID()),
                intoStringArray(runtime, group.getMembers())
        };
        
        return RubyStruct.newStruct(runtime.getGroupStruct(), args, Block.NULL_BLOCK);
    }

    private static IRubyObject intoStringArray(Ruby runtime, String[] members) {
        IRubyObject[] arr = new IRubyObject[members.length];
        for(int i = 0; i<arr.length; i++) {
            arr[i] = runtime.newString(members[i]);
        }
        return RubyArray.newArrayMayCopy(runtime, arr);
    }
    
    @JRubyMethod(module = true)
    public static synchronized IRubyObject sysconf(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        Ruby runtime = context.runtime;
        Sysconf name = Sysconf.valueOf(numericToLong(context, arg));
        POSIX posix = runtime.getPosix();
        posix.errno(0);
        long ret = posix.sysconf(name);

        if (ret == -1) {
            int errno = posix.errno();

            if (errno == Errno.ENOENT.intValue() || errno == 0) {
                return context.nil;
            } else if (errno == Errno.EOPNOTSUPP.intValue()) {
                throw runtime.newNotImplementedError("sysconf() function is unimplemented on this machine");
            } else {
                throw runtime.newErrnoFromLastPOSIXErrno();
            }
        }
        return RubyFixnum.newFixnum(runtime, ret);
    }
    
    @JRubyMethod(module = true)
    public static synchronized IRubyObject confstr(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        Confstr name = Confstr.valueOf(numericToLong(context, arg));
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
        return getpwuid(recv.getRuntime().getCurrentContext(), recv, args);
    }

    @JRubyMethod(optional = 1, checkArity = false, module = true)
    public static synchronized IRubyObject getpwuid(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;

        Arity.checkArgumentCount(context, args, 0, 1);

        POSIX posix = runtime.getPosix();
        IRubyObject oldExc = runtime.getGlobalVariables().get("$!"); // Save $!
        try {
            int uid = args.length == 0 ? posix.getuid() : RubyNumeric.fix2int(args[0]);
            Passwd pwd = posix.getpwuid(uid);
            if(pwd == null) {
                if (Platform.IS_WINDOWS) return context.nil;

                throw argumentError(context, "can't find user for " + uid);
            }
            return setupPasswd(context, pwd);
        } catch (RaiseException re) {
            if (runtime.getNotImplementedError().isInstance(re.getException())) {
                runtime.getGlobalVariables().set("$!", oldExc); // Restore $!
                return runtime.getNil();
            }
            throw re;
        } catch (Exception e) {
            if (runtime.getDebug().isTrue()) {
                runtime.getWarnings().warn(ID.NOT_IMPLEMENTED, "Etc.getpwuid is not supported by JRuby on this platform");
            }
            return context.nil;
        }
    }

    @JRubyMethod(module = true)
    public static synchronized IRubyObject getpwnam(IRubyObject recv, IRubyObject name) {
        Ruby runtime = recv.getRuntime();
        String nam = name.convertToString().toString();
        try {
            ThreadContext context = runtime.getCurrentContext();
            Passwd pwd = runtime.getPosix().getpwnam(nam);
            if (pwd == null) {
                if (Platform.IS_WINDOWS) return context.nil;

                throw argumentError(context, "can't find user for " + nam);
            }
            return setupPasswd(context, pwd);
        } catch (RaiseException e) {
            throw e;
        } catch (Exception e) {
            if (runtime.getDebug().isTrue()) {
                runtime.getWarnings().warn(ID.NOT_IMPLEMENTED, "Etc.getpwnam is not supported by JRuby on this platform");
            }
            return runtime.getNil();
        }
    }

    @JRubyMethod(module = true)
    public static synchronized IRubyObject passwd(IRubyObject recv, Block block) {
        Ruby runtime = recv.getRuntime();
        POSIX posix = runtime.getPosix();
        try {
            // call getpwent to fail early if unsupported
            posix.getpwent();
            if(block.isGiven()) {
                ThreadContext context = runtime.getCurrentContext();
                
                if (!iteratingPasswd.compareAndSet(false, true)) {
                    throw runtime.newRuntimeError("parallel passwd iteration");
                }
                
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
            if (pw != null) {
                return setupPasswd(runtime.getCurrentContext(), pw);
            } else {
                return runtime.getNil();
            }
        } catch (Exception e) {
            if (runtime.getDebug().isTrue()) {
                runtime.getWarnings().warn(ID.NOT_IMPLEMENTED, "Etc.passwd is not supported by JRuby on this platform");
            }
            return runtime.getNil();
        }
    }

    @JRubyMethod(module = true)
    public static synchronized IRubyObject getlogin(IRubyObject recv) {
        Ruby runtime = recv.getRuntime();

        try {
            String login = runtime.getPosix().getlogin();
            if (login != null) {
                return runtime.newString(login);
            }

            login = System.getenv("USER");
            if (login != null) {
                return runtime.newString(login);
            }
            
            return runtime.getNil();
        } catch (Exception e) {
            // fall back on env entry for USER
            return runtime.newString(System.getProperty("user.name"));
        }
    }

    @JRubyMethod(module = true)
    public static synchronized IRubyObject endpwent(IRubyObject recv) {
        Ruby runtime = recv.getRuntime();
        try {
            runtime.getPosix().endpwent();
        } catch (Exception e) {
            if (runtime.getDebug().isTrue()) {
                runtime.getWarnings().warn(ID.NOT_IMPLEMENTED, "Etc.endpwent is not supported by JRuby on this platform");
            }
        }
        return runtime.getNil();
    }

    @JRubyMethod(module = true)
    public static synchronized IRubyObject setpwent(IRubyObject recv) {
        Ruby runtime = recv.getRuntime();
        try {
            runtime.getPosix().setpwent();
        } catch (Exception e) {
            if (runtime.getDebug().isTrue()) {
                runtime.getWarnings().warn(ID.NOT_IMPLEMENTED, "Etc.setpwent is not supported by JRuby on this platform");
            }
        }
        return runtime.getNil();
    }

    @JRubyMethod(module = true)
    public static synchronized IRubyObject getpwent(IRubyObject recv) {
        Ruby runtime = recv.getRuntime();
        try {
            Passwd passwd = runtime.getPosix().getpwent();
            if (passwd != null) {
                return setupPasswd(runtime.getCurrentContext(), passwd);
            } else {
                return runtime.getNil();
            }
        } catch (Exception e) {
            if (runtime.getDebug().isTrue()) {
                runtime.getWarnings().warn(ID.NOT_IMPLEMENTED, "Etc.getpwent is not supported by JRuby on this platform");
            }
            return runtime.getNil();
        }
    }

    @JRubyMethod(module = true)
    public static synchronized IRubyObject getgrnam(IRubyObject recv, IRubyObject name) {
        Ruby runtime = recv.getRuntime();
        String nam = name.convertToString().toString();
        try {
            Group grp = runtime.getPosix().getgrnam(nam);
            if (grp == null) {
                if (Platform.IS_WINDOWS) {  // MRI behavior
                    return runtime.getNil();
                }
                throw runtime.newArgumentError("can't find group for " + nam);
            }
            return setupGroup(runtime, grp);
        } catch (RaiseException e) {
            throw e;
        } catch (Exception e) {
            if (runtime.getDebug().isTrue()) {
                runtime.getWarnings().warn(ID.NOT_IMPLEMENTED, "Etc.getgrnam is not supported by JRuby on this platform");
            }
            return runtime.getNil();
        }
    }

    @Deprecated
    public static synchronized IRubyObject getgrgid(IRubyObject recv, IRubyObject[] args) {
        return getgrgid(recv.getRuntime().getCurrentContext(), recv, args);
    }

    @JRubyMethod(optional = 1, checkArity = false, module = true)
    public static synchronized IRubyObject getgrgid(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;

        int argc = Arity.checkArgumentCount(context, args, 0, 1);

        POSIX posix = runtime.getPosix();

        try {
            int gid = argc == 0 ? posix.getgid() : RubyNumeric.fix2int(args[0]);
            Group gr = posix.getgrgid(gid);
            if(gr == null) {
                if (Platform.IS_WINDOWS) {  // MRI behavior
                    return runtime.getNil();
                }
                throw runtime.newArgumentError("can't find group for " + gid);
            }
            return setupGroup(runtime, gr);
        } catch (RaiseException re) {
            throw re;
        } catch (Exception e) {
            if (runtime.getDebug().isTrue()) {
                runtime.getWarnings().warn(ID.NOT_IMPLEMENTED, "Etc.getgrgid is not supported by JRuby on this platform");
            }
            return runtime.getNil();
        }
    }

    @JRubyMethod(module = true)
    public static synchronized IRubyObject endgrent(IRubyObject recv) {
        Ruby runtime = recv.getRuntime();
        try {
            runtime.getPosix().endgrent();
        } catch (Exception e) {
            if (runtime.getDebug().isTrue()) {
                runtime.getWarnings().warn(ID.NOT_IMPLEMENTED, "Etc.engrent is not supported by JRuby on this platform");
            }
        }
        return runtime.getNil();
    }

    @JRubyMethod(module = true)
    public static synchronized IRubyObject setgrent(IRubyObject recv) {
        Ruby runtime = recv.getRuntime();
        try {
            runtime.getPosix().setgrent();
        } catch (Exception e) {
            if (runtime.getDebug().isTrue()) {
                runtime.getWarnings().warn(ID.NOT_IMPLEMENTED, "Etc.setgrent is not supported by JRuby on this platform");
            }
        }
        return runtime.getNil();
    }

    @JRubyMethod(module = true)
    public static synchronized IRubyObject group(IRubyObject recv, Block block) {
        Ruby runtime = recv.getRuntime();
        POSIX posix = runtime.getPosix();

        try {
            // try to read grent to fail fast
            posix.getgrent();
        } catch (Exception e) {
            if (runtime.getDebug().isTrue()) {
                runtime.getWarnings().warn(ID.NOT_IMPLEMENTED, "Etc.group is not supported by JRuby on this platform");
            }
        }

        if (block.isGiven()) {
            Boolean blocking = (Boolean)recv.getInternalVariables().getInternalVariable("group_blocking");
            if (blocking != null && blocking) {
                throw runtime.newRuntimeError("parallel group iteration");
            }
            try {
                recv.getInternalVariables().setInternalVariable("group_blocking", true);

                ThreadContext context = runtime.getCurrentContext();

                posix.setgrent();
                Group gr;
                while((gr = posix.getgrent()) != null) {
                    block.yield(context, setupGroup(runtime, gr));
                }
            } finally {
                posix.endgrent();
                recv.getInternalVariables().setInternalVariable("group_blocking", false);
            }
        } else {
            Group gr = posix.getgrent();
            if (gr != null) {
                return setupGroup(runtime, gr);
            } else {
                return runtime.getNil();
            }
        }

        return runtime.getNil();
    }

    @JRubyMethod(module = true)
    public static synchronized IRubyObject getgrent(IRubyObject recv) {
        Ruby runtime = recv.getRuntime();
        try {
            Group gr;

            // We synchronize on this class so at least all JRuby instances in this classloader are safe.
            // See jruby/jruby#4057
            synchronized (RubyEtc.class) {
                gr = runtime.getPosix().getgrent();
            }

            if (gr != null) {
                return setupGroup(recv.getRuntime(), gr);
            } else {
                return runtime.getNil();
            }
        } catch (Exception e) {
            if (runtime.getDebug().isTrue()) {
                runtime.getWarnings().warn(ID.NOT_IMPLEMENTED, "Etc.getgrent is not supported by JRuby on this platform");
            }
            return runtime.getNil();
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
        RubyHash uname = RubyHash.newHash(context.runtime);

        uname.op_aset(context,
                Convert.asSymbol(context, "sysname"),
                newString(context, SafePropertyAccessor.getProperty("os.name", "unknown")));
        try {
            uname.op_aset(context,
                    Convert.asSymbol(context, "nodename"),
                    newString(context, InetAddress.getLocalHost().getHostName()));
        } catch (UnknownHostException uhe) {
            uname.op_aset(context,
                    Convert.asSymbol(context, "nodename"),
                    newString(context, "unknown"));
        }
        uname.put(Convert.asSymbol(context, "release"), newString(context, "unknown"));
        uname.put(Convert.asSymbol(context, "version"), newString(context, SafePropertyAccessor.getProperty("os.version")));
        uname.put(Convert.asSymbol(context, "machine"), newString(context, SafePropertyAccessor.getProperty("os.arch")));

        return uname;
    }
    
    private static final AtomicBoolean iteratingPasswd = new AtomicBoolean(false);
}
