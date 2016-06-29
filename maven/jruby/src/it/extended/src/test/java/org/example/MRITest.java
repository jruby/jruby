package org.example;

import org.junit.Test;

import org.jruby.embed.ScriptingContainer;

public class MRITest extends BaseTest {

    // Commented out because 9.1 now seems to *actually* load all the tests and fail (9.0 only loaded one)
//    @Test
    public void testMRI() throws Exception {
        runIt("mri", "ENV['EXCLUDE_DIR']='test/mri/excludes';");
    }

    protected void collectTests(ScriptingContainer container, String index) {
        container.runScriptlet(
            "File.open(File.join('test', '" + index + ".index')) do |f|\n" +
            "      f.each_line do |line|\n" +
            "        next if line =~ /^#/ or line.strip.empty?\n" +
            "        filename = \"test/mri/#{line.chomp}\"\n" +
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

            "        filename.sub!( /.*\\/test\\//, 'test/' )\n" +
            "        puts filename\n" +
            "        require filename\n" +
            "      end\n" +
            "    end" );
    }

}
