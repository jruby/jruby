require 'test/unit'
require 'jruby/jrubyc'
require 'fileutils'

class TestLoadCompiledRuby < Test::Unit::TestCase
  FILENAME = 'test_load_compiled_ruby.rb'
  COMPILED = 'test_load_compiled_ruby.class'
  def test_load_compiled_ruby
    begin
      File.open(FILENAME, 'w') do |f|
        f.write('$test_load_compiled_ruby = true')
      end
      assert File.exist? FILENAME

      JRubyCompiler::compile_argv([FILENAME])
      FileUtils.rm_f(FILENAME)
      assert !(File.exist? FILENAME)
      assert File.exist? COMPILED

      $:.unshift('.')
      load FILENAME
      assert $test_load_compiled_ruby
    ensure
      FileUtils.rm_f(FILENAME)
      FileUtils.rm_f(COMPILED)
    end
  end
end