package org.jruby.management;

import java.lang.reflect.Constructor;
import org.jruby.Ruby;
import org.jruby.compiler.JITCompilerMBean;

public class BeanManagerFactory {
    private static final Class BeanManagerImpl;
    private static final Constructor BeanManagerImpl_constructor;

    static {
        Class bm = null;
        Constructor bmc = null;
        try {
            bm = Class.forName("org.jruby.management.BeanManagerImpl");
            bmc = bm.getConstructor(Ruby.class, boolean.class);
        } catch (Exception e) {
        }
        BeanManagerImpl = bm;
        BeanManagerImpl_constructor = bmc;
    }

    public static BeanManager create(Ruby runtime, boolean managementEnabled) {
        if (BeanManagerImpl_constructor != null) {
            try {
                return (BeanManager)BeanManagerImpl_constructor.newInstance(runtime, managementEnabled);
            } catch (Exception e) {
                // do nothing, return dummy version below
            }
        }

        return new DummyBeanManager();
    }

    private static class DummyBeanManager implements BeanManager {
        public void register(JITCompilerMBean jitCompiler) {}
        public void register(ConfigMBean config) {}
        public void register(ParserStatsMBean parserStats) {}
        public void register(MethodCacheMBean methodCache) {}
        public void register(ClassCacheMBean classCache) {}
        public void unregisterClassCache() {}
        public void unregisterCompiler() {}
        public void unregisterConfig() {}
        public void unregisterMethodCache() {}
        public void unregisterParserStats() {}
    }
}
