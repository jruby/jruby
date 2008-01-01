package org.jruby.ext.posix;

public interface Passwd {
    public String getLoginName();
    public String getPassword();
    public long getUID();
    public long getGID();
    public int getPasswdChangeTime();
    public String getAccessClass();
    public String getGECOS();
    public String getHome();
    public String getShell();
}
