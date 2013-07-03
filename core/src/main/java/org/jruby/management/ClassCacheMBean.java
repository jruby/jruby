package org.jruby.management;

public interface ClassCacheMBean {
    public boolean isFull();
    public int getClassLoadCount();
    public int getLiveClassCount();
    public int getClassReuseCount();
    public void flush();
}
