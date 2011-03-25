package org.jruby.management;

import org.jruby.compiler.JITCompilerMBean;

public interface BeanManager {

    void register(JITCompilerMBean jitCompiler);

    void register(ConfigMBean config);

    void register(ParserStatsMBean parserStats);

    void register(MethodCacheMBean methodCache);

    void register(ClassCacheMBean classCache);

    void register(Runtime runtime);

    void unregisterClassCache();

    void unregisterCompiler();

    void unregisterConfig();

    void unregisterMethodCache();

    void unregisterParserStats();
    
    void unregisterRuntime();

}
