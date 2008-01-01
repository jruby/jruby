package org.jruby.ext.posix;

import com.sun.jna.Structure;

public class NativePasswd extends Structure implements Passwd {
    public String pw_name;   // user name
    public String pw_passwd; // password (encrypted)
    public int pw_uid;       // user id
    public int pw_gid;       // user id
    public int pw_change;    // password change time
    public String pw_class;  // user access class
    public String pw_gecos;  // login info
    public String pw_dir;    // home directory
    public String pw_shell;  // default shell
    public int pw_expire;    // account expiration
    public int unused;       // padding
    
    public String getAccessClass() {
        return pw_class;
    }
    public String getGECOS() {
        return pw_gecos;
    }
    public long getGID() {
        return pw_gid & 0xffffffff;
    }
    public String getHome() {
        return pw_dir;
    }
    public String getLoginName() {
        return pw_name;
    }
    public int getPasswdChangeTime() {
        return pw_change;
    }
    public String getPassword() {
        return pw_passwd;
    }
    public String getShell() {
        return pw_shell;
    }
    public long getUID() {
        return pw_uid  & 0xffffffff;
    }
}
