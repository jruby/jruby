package org.jruby.ext.posix;

public class JavaPasswd implements Passwd {
    private POSIXHandler handler;

    public JavaPasswd(POSIXHandler handler) {
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
        handler.unimplementedError("passwd.pw_gid unimplemented");

        return -1;
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
        handler.unimplementedError("passwd.pw_uid unimplemented");
        
        return -1;
    }

    public int getExpire() {
        handler.unimplementedError("passwd.expire unimplemented");
        
        return -1;
    }
}
