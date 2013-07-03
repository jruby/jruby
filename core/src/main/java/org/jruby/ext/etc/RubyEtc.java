package org.jruby.ext.etc;

import java.util.concurrent.atomic.AtomicBoolean;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.exceptions.RaiseException;
import jnr.posix.Passwd;
import jnr.posix.Group;
import jnr.posix.POSIX;
import jnr.posix.util.Platform;
import org.jruby.CompatVersion;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.RubyStruct;
import org.jruby.ext.rbconfig.RbConfigLibrary;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.NormalizedFile;

@JRubyModule(name="Etc")
public class RubyEtc {
    public static RubyModule createEtcModule(Ruby runtime) {
        RubyModule etcModule = runtime.defineModule("Etc");

        runtime.setEtc(etcModule);
        
        etcModule.defineAnnotatedMethods(RubyEtc.class);
        
        definePasswdStruct(runtime);
        defineGroupStruct(runtime);
        
        return etcModule;
    }
    
    private static void definePasswdStruct(Ruby runtime) {
        IRubyObject[] args = new IRubyObject[] {
                runtime.newString("Passwd"),
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
        
        runtime.setPasswdStruct(RubyStruct.newInstance(runtime.getStructClass(), args, Block.NULL_BLOCK));
        if (runtime.is1_9()) {
            runtime.getEtc().defineConstant("Passwd", runtime.getPasswdStruct());
        }
    }

    private static void defineGroupStruct(Ruby runtime) {
        IRubyObject[] args = new IRubyObject[] {
                runtime.newString("Group"),
                runtime.newSymbol("name"),
                runtime.newSymbol("passwd"),
                runtime.newSymbol("gid"),
                runtime.newSymbol("mem")
        };
        
        runtime.setGroupStruct(RubyStruct.newInstance(runtime.getStructClass(), args, Block.NULL_BLOCK));
        if (runtime.is1_9()) {
            runtime.getEtc().defineConstant("Group", runtime.getGroupStruct());
        }
    }
    
    private static IRubyObject setupPasswd(Ruby runtime, Passwd passwd) {
        IRubyObject[] args = new IRubyObject[] {
                runtime.newString(passwd.getLoginName()),
                runtime.newString(passwd.getPassword()),
                runtime.newFixnum(passwd.getUID()),
                runtime.newFixnum(passwd.getGID()),
                runtime.newString(passwd.getGECOS()),
                runtime.newString(passwd.getHome()),
                runtime.newString(passwd.getShell()),
                runtime.newFixnum(passwd.getPasswdChangeTime()),
                runtime.newString(passwd.getAccessClass()),
                runtime.newFixnum(passwd.getExpire())

        };
        
        return RubyStruct.newStruct(runtime.getPasswdStruct(), args, Block.NULL_BLOCK);
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
        return runtime.newArrayNoCopy(arr);
    }


    @JRubyMethod(name = "getpwuid", optional=1, module = true)
    public static IRubyObject getpwuid(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
        POSIX posix = runtime.getPosix();
        try {
            int uid = args.length == 0 ? posix.getuid() : RubyNumeric.fix2int(args[0]);
            Passwd pwd = posix.getpwuid(uid);
            if(pwd == null) {
                if (Platform.IS_WINDOWS) {  // MRI behavior
                    return recv.getRuntime().getNil();
                }
                throw runtime.newArgumentError("can't find user for " + uid);
            }
            return setupPasswd(runtime, pwd);
        } catch (RaiseException re) {
            if (runtime.getNotImplementedError().isInstance(re.getException())) {
                return runtime.getNil();
            }
            throw re;
        } catch (Exception e) {
            if (runtime.getDebug().isTrue()) {
                runtime.getWarnings().warn(ID.NOT_IMPLEMENTED, "Etc.getpwuid is not supported by JRuby on this platform");
            }
            return runtime.getNil();
        }
    }

    @JRubyMethod(name = "getpwnam", required=1, module = true)
    public static IRubyObject getpwnam(IRubyObject recv, IRubyObject name) {
        Ruby runtime = recv.getRuntime();
        String nam = name.convertToString().toString();
        try {
            Passwd pwd = runtime.getPosix().getpwnam(nam);
            if(pwd == null) {
                if (Platform.IS_WINDOWS) {  // MRI behavior
                    return runtime.getNil();
                }
                throw runtime.newArgumentError("can't find user for " + nam);
            }
            return setupPasswd(recv.getRuntime(), pwd);
        } catch (Exception e) {
            if (runtime.getDebug().isTrue()) {
                runtime.getWarnings().warn(ID.NOT_IMPLEMENTED, "Etc.getpwnam is not supported by JRuby on this platform");
            }
            return runtime.getNil();
        }
    }

    @JRubyMethod(module = true)
    public static IRubyObject passwd(IRubyObject recv, Block block) {
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
                        block.yield(context, setupPasswd(runtime, pw));
                    }
                } finally {
                    posix.endpwent();
                    iteratingPasswd.set(false);
                }
            }

            Passwd pw = posix.getpwent();
            if (pw != null) {
                return setupPasswd(runtime, pw);
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

    @JRubyMethod(name = "getlogin", module = true)
    public static IRubyObject getlogin(IRubyObject recv) {
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

    @JRubyMethod(name = "endpwent", module = true)
    public static IRubyObject endpwent(IRubyObject recv) {
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

    @JRubyMethod(name = "setpwent", module = true)
    public static IRubyObject setpwent(IRubyObject recv) {
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

    @JRubyMethod(name = "getpwent", module = true)
    public static IRubyObject getpwent(IRubyObject recv) {
        Ruby runtime = recv.getRuntime();
        try {
            Passwd passwd = runtime.getPosix().getpwent();
            if (passwd != null) {
                return setupPasswd(runtime, passwd);
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

    @JRubyMethod(name = "getgrnam", required=1, module = true)
    public static IRubyObject getgrnam(IRubyObject recv, IRubyObject name) {
        Ruby runtime = recv.getRuntime();
        String nam = name.convertToString().toString();
        try {
            Group grp = runtime.getPosix().getgrnam(nam);
            if(grp == null) {
                if (Platform.IS_WINDOWS) {  // MRI behavior
                    return runtime.getNil();
                }
                throw runtime.newArgumentError("can't find group for " + nam);
            }
            return setupGroup(runtime, grp);
        } catch (Exception e) {
            if (runtime.getDebug().isTrue()) {
                runtime.getWarnings().warn(ID.NOT_IMPLEMENTED, "Etc.getgrnam is not supported by JRuby on this platform");
            }
            return runtime.getNil();
        }
    }

    @JRubyMethod(name = "getgrgid", optional=1, module = true)
    public static IRubyObject getgrgid(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
        POSIX posix = runtime.getPosix();

        try {
            int gid = args.length == 0 ? posix.getgid() : RubyNumeric.fix2int(args[0]);
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

    @JRubyMethod(name = "endgrent", module = true)
    public static IRubyObject endgrent(IRubyObject recv) {
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
    public static IRubyObject setgrent(IRubyObject recv) {
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
    public static IRubyObject group(IRubyObject recv, Block block) {
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

    @JRubyMethod(name = "getgrent", module = true)
    public static IRubyObject getgrent(IRubyObject recv) {
        Ruby runtime = recv.getRuntime();
        try {
            Group gr = runtime.getPosix().getgrent();
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
    
    @JRubyMethod(name = "systmpdir", module = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject systmpdir(ThreadContext context, IRubyObject recv) {
        Ruby runtime = context.getRuntime();
        ByteList tmp = ByteList.create("/tmp"); // default for all platforms except Windows
        
        if (Platform.IS_WINDOWS) {
            String commonAppData = System.getenv("CSIDL_COMMON_APPDATA");
            // TODO: need fallback mechanism
            if (commonAppData != null) tmp = ByteList.create(commonAppData);
        }
        RubyString ret = RubyString.newString(runtime, tmp, runtime.getDefaultExternalEncoding());
        ret.untaint(context);
        ret.trust(context);
        
        return ret;
    }
    
    @JRubyMethod(name = "sysconfdir", module = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject sysconfdir(ThreadContext context, IRubyObject recv) {
        Ruby runtime = context.getRuntime();
        ByteList tmp = ByteList.create(RbConfigLibrary.getSysConfDir(runtime)); // default for all platforms except Windows
        
        if (Platform.IS_WINDOWS) {
            String localAppData = System.getenv("CSIDL_LOCAL_APPDATA");
            // TODO: need fallback mechanism
            if (localAppData != null) tmp = ByteList.create(localAppData);
        }
        RubyString ret = RubyString.newString(runtime, tmp, runtime.getDefaultExternalEncoding());
        ret.untaint(context);
        ret.trust(context);
        
        return ret;
    }
    
    private static final AtomicBoolean iteratingPasswd = new AtomicBoolean(false);
}
