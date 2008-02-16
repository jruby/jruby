package org.jruby;

import org.jruby.anno.JRubyMethod;
import org.jruby.ext.posix.Passwd;
import org.jruby.runtime.Block;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyEtc {
    public static RubyModule createEtcModule(Ruby runtime) {
        RubyModule etcModule = runtime.defineModule("Etc");

        runtime.setEtc(etcModule);
        
        etcModule.defineAnnotatedMethods(RubyEtc.class);
        
        definePasswdStruct(runtime);
        
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

    // "getgrnam", "setgrent", "group", "endgrent", "getpwuid", "passwd", "getgrent", "getpwnam", "getgrgid"




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
        
        return setupPasswd(recv.getRuntime(), passwd);
    }
}
