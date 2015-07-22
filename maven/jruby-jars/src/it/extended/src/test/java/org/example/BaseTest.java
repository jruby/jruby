package org.example;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.io.File;
import java.io.StringWriter;

import org.jruby.embed.LocalContextScope;
import org.jruby.embed.ScriptingContainer;

import org.junit.Before;
import org.junit.Test;

public class BaseTest {

    private final String basedir = new File( System.getProperty("basedir"), "../../../../../" ).getAbsolutePath();

    private ScriptingContainer newScriptingContainer() {
        ScriptingContainer container = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
        container.setCurrentDirectory(basedir);
    	container.getProvider().getRubyInstanceConfig().setLoadPaths(Arrays.asList(".", "test", "test/mri", "test/mri/ruby"));
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
                               "        next if line =~ /^#/ or line.strip.empty?\n" +
                               "        filename = \"test/mri/#{line.chomp}\"\n" +
                               "        filename = \"test/jruby/#{line.chomp}.rb\" unless File.exist? filename\n" +
                               "        filename = \"test/#{line.chomp}.rb\" unless File.exist? filename\n" +
                               "        next unless File.file? filename\n" +
                               "        next if filename =~ /mri\\/net\\/http\\//\n" +
                               "        next if filename =~ /mri\\/ruby\\/test_class/\n" +
                               "        next if filename =~ /mri\\/ruby\\/test_io/\n" +
                               "        next if filename =~ /mri\\/ruby\\/test_econv/\n" +
                               // TODO find a way to obey the minitest/excludes and get those back
                               "        next if filename =~ /psych\\/test_encoding.rb/\n" +
                               "        next if filename =~ /psych\\/test_parser.rb/\n" +
                               "        next if filename =~ /psych\\/test_psych.rb/\n" +
                               "        next if filename =~ /psych\\/visitors\\/test_yaml_tree.rb/\n" +
                               "        next if filename =~ /psych\\/test_document.rb/\n" +
                               "        next if filename =~ /psych\\/test_tree_builder.rb/\n" +
                               "        next if filename =~ /psych\\/test_date_time.rb/\n" +
                               "        next if filename =~ /psych\\/test_nil.rb/\n" +
                               // TODO file an issue or so
                               "        next if filename =~ /test_load_compiled_ruby.rb/\n" +
                               "        next if filename =~ /compiler\\/test_jrubyc.rb/\n" +
                               // TODO remove the following after fix of #2215
                               "        next if filename =~ /test_jar_on_load_path.rb/\n" +
                               "        next if filename =~ /test_file.rb/\n" +
                               "        filename.sub!( /.*\\/test\\//, 'test/' )\n" +
                               "        puts filename\n" +
                               "        require filename\n" +
                               "      end\n" +
                               "    end");
    }

    void runIt(String index) throws Exception {
        runIt(index, null);
    }

    void runIt(String index, String script) throws Exception {
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

    // @Test
    // public void test() throws Exception {
    // 	runIt(null, "require '/home/christian/projects/active/maven/jruby/test/jruby/test_command_line_switches.rb'");
    // }

}
