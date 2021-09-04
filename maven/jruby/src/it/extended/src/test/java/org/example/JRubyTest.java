package org.example;

import org.junit.Test;

import org.jruby.embed.ScriptingContainer;

public class JRubyTest extends BaseTest {

    @Test
    public void testJRuby() throws Exception {
        runIt("jruby");
    }

    protected void collectTests(ScriptingContainer container, String index) {
        container.runScriptlet(
            "File.open(File.join('test', '" + index + ".index')) do |f|\n" +
            "      f.each_line do |line|\n" +
            "        next if line =~ /^#/ or line.strip.empty?\n" +
            "        filename = \"test/jruby/#{line.chomp}.rb\"\n" +
            "        filename = \"test/#{line.chomp}.rb\" unless File.exist? filename\n" +
            "        next unless File.file? filename\n" +

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
            "    end" );
    }

}
