package org.jruby.management;

import org.jruby.runtime.CacheMap;

public class MethodCache implements MethodCacheMBean {
    private CacheMap cacheMap;
    
    public MethodCache(CacheMap cacheMap) {
        this.cacheMap = cacheMap;
    }
    
    public int getAddCount() {
        return cacheMap.getAddCount();
    }
    
    public int getRemoveCount() {
        return cacheMap.getRemoveCount();
    }
    
    public int getModuleIncludeCount() {
        return cacheMap.getModuleIncludeCount();
    }
    
    public int getModuleTriggeredRemoveCount() {
        return cacheMap.getModuleTriggeredRemoveCount();
    }
    
    public int getFlushCount() {
        return cacheMap.getFlushCount();
    }
    
    public void flush() {
        cacheMap.flush();
    }
}
