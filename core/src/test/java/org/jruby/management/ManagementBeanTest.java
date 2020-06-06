package org.jruby.management;

import java.util.Arrays;
import java.util.Collection;
import java.util.TreeSet;

import org.jruby.embed.LocalContextScope;
import org.jruby.embed.ScriptingContainer;
import org.junit.Assert;
import org.junit.Test;

public class ManagementBeanTest {

    @Test
    public void testUniquenessOfBeanNames() throws Exception {
        System.setProperty("jruby.management.enabled", "true");
        final Collection<String> result = new TreeSet<String>();
        // if concurrent runtime do produce unique names we are good
        // see https://github.com/jruby/jruby/issues/2582
        int number = java.lang.Runtime.getRuntime().availableProcessors();
        Thread[] threads = new Thread[number];
        for( int i = 0; i < number; i++) {
            threads[i] = new Thread(new Runnable(){
                public void run() {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                    }
                    ScriptingContainer instance = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
                    String[] names = instance.runScriptlet( "mbs = java.lang.management.ManagementFactory.getPlatformMBeanServer;"
                        + "h = mbs.queryNames( nil, nil);"
                        + "hh = h.select { |a| a.to_s.start_with?( 'org.jruby:type=Runtime' ) };"
                        + "hh.collect { |a| a.to_s }.join( '_' )" ).toString().split("_");
                    synchronized(result){
                        result.addAll(Arrays.asList(names));
                    }
                    instance.terminate();
                }
            });
            threads[i].start();
        }
        for( int i = 0; i < number; i++) {
            threads[i].join();
        }
        Assert.assertEquals( number * 5, result.size() );
    }
}