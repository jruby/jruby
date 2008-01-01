package org.jruby.ext.posix;

public class JavaPasswd extends NativePasswd {
    private LibC libc;
    private POSIXHandler handler;

    public JavaPasswd(LibC libc, POSIXHandler handler) {
        this.libc = libc;
        this.handler = handler;
    }
    
    public String getAccessClass() {
        handler.unimplementedError("passwd.pw_access unimplemented");
        
        return null;
    }

    public String getGECOS() {
        handler.unimplementedError("passwd.pw_gecos unimplemented");
        
        return null;
    }

    public long getGID() {
        return libc.getgid();
    }

    public String getHome() {
        return System.getProperty("user.home");
    }

    public String getLoginName() {
        return System.getProperty("user.name");
    }

    public int getPasswdChangeTime() {
        handler.unimplementedError("passwd.pw_change unimplemented");

        return 0;
    }

    public String getPassword() {
        handler.unimplementedError("passwd.pw_passwd unimplemented");
        
        return null;
    }

    public String getShell() {
        handler.unimplementedError("passwd.pw_env unimplemented");
        
        return null;
    }

    public long getUID() {
        return libc.getuid();
    }
}
