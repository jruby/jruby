package org.jruby.management;

import java.lang.ref.SoftReference;

import org.jruby.runtime.CacheMap;
import org.jruby.runtime.callsite.CachingCallSite;

public class MethodCache implements MethodCacheMBean {
    private final SoftReference<CacheMap> cacheMap;
    
    public MethodCache(CacheMap cacheMap) {
        this.cacheMap = new SoftReference<CacheMap>(cacheMap);
    }
    
    public int getAddCount() {
        return cacheMap.get().getAddCount();
    }
    
    public int getRemoveCount() {
        return cacheMap.get().getRemoveCount();
    }
    
    public int getModuleIncludeCount() {
        return cacheMap.get().getModuleIncludeCount();
    }
    
    public int getModuleTriggeredRemoveCount() {
        return cacheMap.get().getModuleTriggeredRemoveCount();
    }
    
    public int getFlushCount() {
        return cacheMap.get().getFlushCount();
    }
    
    public int getCallSiteCount() {
        return CachingCallSite.totalCallSites;
    }
    
    public int getFailedCallSiteCount() {
        return CachingCallSite.failedCallSites;
    }
    
    public void flush() {
        cacheMap.get().flush();
    }
}
