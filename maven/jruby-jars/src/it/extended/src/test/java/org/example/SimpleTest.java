package org.example;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.io.File;
import java.io.StringWriter;

import org.jruby.embed.LocalContextScope;
import org.jruby.embed.ScriptingContainer;

import org.junit.Before;
import org.junit.Test;

public class SimpleTest {

    private final String basedir = new File( System.getProperty("basedir"), "../../../../../" ).getAbsolutePath();

    private ScriptingContainer newScriptingContainer() {
	ScriptingContainer container = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
	container.setCurrentDirectory(basedir);
    	container.getProvider().getRubyInstanceConfig().setJRubyHome("uri:classloader://META-INF/jruby.home");
    	container.getProvider().getRubyInstanceConfig().setLoadPaths(Arrays.asList(".", "test", "test/externals/ruby1.9", "test/externals/ruby1.9/ruby"));
	container.runScriptlet("ENV['GEM_PATH']='lib/ruby/gems/shared'");
	return container;
    }

    private void runTests(ScriptingContainer container) throws Exception {
	container.getProvider().getRuntime().tearDown(true);
	container.terminate();
    }

    private void collectTests(ScriptingContainer container, String index) throws Exception {
	container.runScriptlet("File.open(File.join('test', '" + index + ".index')) do |f|\n" +
"      f.each_line.each do |line|\n" +
"        filename = \"test/#{line.chomp}.rb\"\n" +
"        next unless File.exist? filename\n" +
"        next if filename =~ /externals\\/ruby1.9\\/ruby\\/test_class/\n" +
"        next if filename =~ /externals\\/ruby1.9\\/ruby\\/test_io/\n" +
"        next if filename =~ /externals\\/ruby1.9\\/ruby\\/test_econv/\n" +
"        next if filename =~ /externals\\/ruby1.9\\/test_open3/\n" +
	 // TODO file an issue or so
"        next if filename =~ /test_load_compiled_ruby.rb/\n" +
         // TODO remove the following after fix of #2215
"        next if filename =~ /test_jar_on_load_path.rb/\n" +
"        next if filename =~ /test_file.rb/\n" +
"        filename.sub!( /.*\\/test\\//, 'test/' )\n" +
"        puts filename\n" +
"        require filename\n" +
"      end\n" +
"    end");
    }

    private void runIt(String index) throws Exception {
	runIt(index, null);
    }

    private void runIt(String index, String script) throws Exception {
	ClassLoader cl = Thread.currentThread().getContextClassLoader();
	try {
	    //Thread.currentThread().setContextClassLoader();
	    System.err.println("\n\nrunning --------- " + index + "\n");
	    ScriptingContainer container = newScriptingContainer();
	    if (script != null) container.runScriptlet( script );
	    if (index != null) collectTests(container, index);
	    runTests(container);
	}
	finally {
	    Thread.currentThread().setContextClassLoader(cl);
	}
    }

    @Test
    public void testObjectspace() throws Exception {
	runIt("objectspace");
    }

    @Test
    public void testSlow() throws Exception {
	runIt("slow");
    }

    // broken on travis and really hard to debug as it works locally
    //@Test
    public void testMRI() throws Exception {
	runIt("mri.1.9", "ENV['EXCLUDE_DIR']='test/externals/ruby1.9/excludes';require 'minitest/excludes'");
    }

    @Test
    public void testRubicon() throws Exception {
	runIt("rubicon.1.9");
    }

    @Test
    public void testJRuby() throws Exception {
	runIt("jruby.1.9");
    }

    // @Test
    // public void test() throws Exception {
    // 	runIt(null, "require 'test/test_load_compiled_ruby.rb'");
    // }

}
