package org.jruby.management;

import org.jruby.Ruby;

public class ClassCache implements ClassCacheMBean {
    private final Ruby ruby;
    
    public ClassCache(Ruby ruby) {
        this.ruby = ruby;
    }

    public boolean isFull() {
        return ruby.getInstanceConfig().getClassCache().isFull();
    }

    public int getClassLoadCount() {
        return ruby.getInstanceConfig().getClassCache().getClassLoadCount();
    }

    public int getLiveClassCount() {
        return ruby.getInstanceConfig().getClassCache().getLiveClassCount();
    }

    public int getClassReuseCount() {
        return ruby.getInstanceConfig().getClassCache().getClassReuseCount();
    }

    public void flush() {
        ruby.getInstanceConfig().getClassCache().flush();
    }
}
