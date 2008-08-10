package org.jruby.management;

public interface MethodCacheMBean {
    public int getAddCount();
    public int getRemoveCount();
    public int getModuleIncludeCount();
    public int getModuleTriggeredRemoveCount();
    public int getFlushCount();
    public int getCallSiteCount();
    public int getFailedCallSiteCount();
    public void flush();
}
