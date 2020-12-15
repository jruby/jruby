require 'test/unit'
require 'fileutils'

class TestLoadCompiledRuby < Test::Unit::TestCase
  FILENAME = 'test_load_compiled_ruby_script.rb'
  COMPILED = 'test_load_compiled_ruby_script.class'

  def test_load_compiled_ruby
    unless org.jruby.util.cli.Options::AOT_LOADCLASSES.load
      omit "loading .class not enabled; this test will be skipped"
    end

    require 'jruby/jrubyc'
    begin
      File.open(FILENAME, 'w') do |f|
        f.write('$test_load_compiled_ruby = true')
      end
      assert File.exist? FILENAME

      JRuby::Compiler::compile_argv([FILENAME])
      FileUtils.rm_f(FILENAME)
      assert !(File.exist? FILENAME)
      assert File.exist? COMPILED

      $:.unshift(File.dirname(File.expand_path(FILENAME)))
      load FILENAME
      assert $test_load_compiled_ruby
    ensure
      $:.shift
      FileUtils.rm_f(FILENAME)
      FileUtils.rm_f(COMPILED)
    end
  end
end
