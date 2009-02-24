/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.management;

import org.jruby.compiler.JITCompilerMBean;

/**
 *
 * @author headius
 */
public interface BeanManager {

    void register(JITCompilerMBean jitCompiler);

    void register(ConfigMBean config);

    void register(ParserStatsMBean parserStats);

    void register(MethodCacheMBean methodCache);

    void register(ClassCacheMBean classCache);

    void unregisterClassCache();

    void unregisterCompiler();

    void unregisterConfig();

    void unregisterMethodCache();

    void unregisterParserStats();

}
