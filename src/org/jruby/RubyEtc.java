package org.jruby;

import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.ext.posix.Passwd;
import org.jruby.ext.posix.Group;
import org.jruby.ext.posix.POSIX;
import org.jruby.ext.posix.util.Platform;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

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
        int uid = args.length == 0 ? posix.getuid() : RubyNumeric.fix2int(args[0]);
        Passwd pwd = posix.getpwuid(uid);
        if(pwd == null) {
            if (Platform.IS_WINDOWS) {  // MRI behavior
                return recv.getRuntime().getNil();
            }
            throw runtime.newArgumentError("can't find user for " + uid);
        }
        return setupPasswd(runtime, pwd);
    }

    @JRubyMethod(name = "getpwnam", required=1, module = true)
    public static IRubyObject getpwnam(IRubyObject recv, IRubyObject name) {
        String nam = name.convertToString().toString();
        Passwd pwd = recv.getRuntime().getPosix().getpwnam(nam);
        if(pwd == null) {
            if (Platform.IS_WINDOWS) {  // MRI behavior
                return recv.getRuntime().getNil();
            }
            throw recv.getRuntime().newArgumentError("can't find user for " + nam);
        }
        return setupPasswd(recv.getRuntime(), pwd);
    }

    @JRubyMethod(name = "passwd", module = true, frame=true)
    public static IRubyObject passwd(IRubyObject recv, Block block) {
        Ruby runtime = recv.getRuntime();
        POSIX posix = runtime.getPosix();
        if(block.isGiven()) {
            ThreadContext context = runtime.getCurrentContext();
            posix.setpwent();
            Passwd pw;
            while((pw = posix.getpwent()) != null) {
                block.yield(context, setupPasswd(runtime, pw));
            }
            posix.endpwent();
        }

        Passwd pw = posix.getpwent();
        if (pw != null) {
            return setupPasswd(runtime, pw);
        } else {
            return runtime.getNil();
        }
    }

    @JRubyMethod(name = "getlogin", module = true)
    public static IRubyObject getlogin(IRubyObject recv) {
        Ruby runtime = recv.getRuntime();
        
        String login = runtime.getPosix().getlogin();
        
        if (login != null) {
            return runtime.newString(login);
        } else {
            return runtime.getNil();
        }
    }

    @JRubyMethod(name = "endpwent", module = true)
    public static IRubyObject endpwent(IRubyObject recv) {
        Ruby runtime = recv.getRuntime();
        runtime.getPosix().endpwent();
        return runtime.getNil();
    }

    @JRubyMethod(name = "setpwent", module = true)
    public static IRubyObject setpwent(IRubyObject recv) {
        Ruby runtime = recv.getRuntime();
        runtime.getPosix().setpwent();
        return runtime.getNil();
    }

    @JRubyMethod(name = "getpwent", module = true)
    public static IRubyObject getpwent(IRubyObject recv) {
        Ruby runtime = recv.getRuntime();
        Passwd passwd = runtime.getPosix().getpwent();
        if (passwd != null) {
            return setupPasswd(recv.getRuntime(), passwd);
        } else {
            return runtime.getNil();
        }
    }

    @JRubyMethod(name = "getgrnam", required=1, module = true)
    public static IRubyObject getgrnam(IRubyObject recv, IRubyObject name) {
        String nam = name.convertToString().toString();
        Group grp = recv.getRuntime().getPosix().getgrnam(nam);
        if(grp == null) {
            if (Platform.IS_WINDOWS) {  // MRI behavior
                return recv.getRuntime().getNil();
            }
            throw recv.getRuntime().newArgumentError("can't find group for " + nam);
        }
        return setupGroup(recv.getRuntime(), grp);
    }

    @JRubyMethod(name = "getgrgid", optional=1, module = true)
    public static IRubyObject getgrgid(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
        POSIX posix = runtime.getPosix();
        int gid = args.length == 0 ? posix.getgid() : RubyNumeric.fix2int(args[0]);
        Group gr = posix.getgrgid(gid);
        if(gr == null) {
            if (Platform.IS_WINDOWS) {  // MRI behavior
                return runtime.getNil();
            }
            throw runtime.newArgumentError("can't find group for " + gid);
        }
        return setupGroup(runtime, gr);
    }

    @JRubyMethod(name = "endgrent", module = true)
    public static IRubyObject endgrent(IRubyObject recv) {
        Ruby runtime = recv.getRuntime();
        runtime.getPosix().endgrent();
        return runtime.getNil();
    }

    @JRubyMethod(name = "setgrent", module = true)
    public static IRubyObject setgrent(IRubyObject recv) {
        Ruby runtime = recv.getRuntime();
        runtime.getPosix().setgrent();
        return runtime.getNil();
    }

    @JRubyMethod(name = "group", module = true, frame=true)
    public static IRubyObject group(IRubyObject recv, Block block) {
        Ruby runtime = recv.getRuntime();
        POSIX posix = runtime.getPosix();
        if(block.isGiven()) {
            ThreadContext context = runtime.getCurrentContext();
            posix.setgrent();
            Group gr;
            while((gr = posix.getgrent()) != null) {
                block.yield(context, setupGroup(runtime, gr));
            }
            posix.endgrent();
        }

        Group gr = posix.getgrent();
        if (gr != null) {
            return setupGroup(runtime, gr);
        } else {
            return runtime.getNil();
        }
    }

    @JRubyMethod(name = "getgrent", module = true)
    public static IRubyObject getgrent(IRubyObject recv) {
        Ruby runtime = recv.getRuntime();
        Group gr = runtime.getPosix().getgrent();
        if (gr != null) {
            return setupGroup(recv.getRuntime(), gr);
        } else {
            return runtime.getNil();
        }        
    }
}
