package org.jruby.management;

import java.lang.ref.SoftReference;

import org.jruby.Ruby;

public class ClassCache implements ClassCacheMBean {
    private final SoftReference<Ruby> ruby;
    
    public ClassCache(Ruby ruby) {
        this.ruby = new SoftReference<Ruby>(ruby);
    }

    public boolean isFull() {
        return ruby.get().getInstanceConfig().getClassCache().isFull();
    }

    public int getClassLoadCount() {
        return ruby.get().getInstanceConfig().getClassCache().getClassLoadCount();
    }

    public int getLiveClassCount() {
        return ruby.get().getInstanceConfig().getClassCache().getLiveClassCount();
    }

    public int getClassReuseCount() {
        return ruby.get().getInstanceConfig().getClassCache().getClassReuseCount();
    }

    public void flush() {
        ruby.get().getInstanceConfig().getClassCache().flush();
    }
}
