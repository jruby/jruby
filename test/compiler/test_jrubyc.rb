require 'test/unit'
require 'stringio'
require 'tempfile'
require 'fileutils'
require 'java'
require 'jruby/jrubyc'

class TestJrubyc < Test::Unit::TestCase
  def setup
    @tempfile = Tempfile.open("test_jrubyc")
    @old_stdout = $stdout.dup
    $stdout.reopen @tempfile
    $stdout.sync = true
  end

  def teardown
    FileUtils.rm_rf(["foo", "ruby"])
    $stdout.reopen(@old_stdout)
  end

  def test_basic
    begin
      JRubyCompiler::compile_argv([__FILE__])
      output = File.read(@tempfile.path)

      assert_equal(
        "Compiling #{__FILE__} to class test/compiler/test_jrubyc\n",
        output)

      assert(File.exist?("test/compiler/test_jrubyc.class"))
    ensure
      File.delete("test/compiler/test_jrubyc.class") rescue nil
    end
  end
  
  def test_target
    tempdir = File.dirname(@tempfile.path)
    JRubyCompiler::compile_argv(["-t", tempdir, __FILE__])
    output = File.read(@tempfile.path)

    assert_equal(
      "Compiling #{__FILE__} to class test/compiler/test_jrubyc\n",
      output)

    assert(File.exist?(tempdir + "/test/compiler/test_jrubyc.class"))
    FileUtils.rm_rf(tempdir + "/test/compiler/test_jrubyc.class")
  end
  
  def test_bad_target
    begin
      JRubyCompiler::compile_argv(["-t", "does_not_exist", __FILE__])
    rescue Exception => e
    end

    assert(e)
    assert_equal(
      "Target dir not found: does_not_exist",
      e.message)
  end
  
  def test_prefix
    JRubyCompiler::compile_argv(["-p", "foo", __FILE__])
    output = File.read(@tempfile.path)

    assert_equal(
      "Compiling #{__FILE__} to class foo/test/compiler/test_jrubyc\n",
      output)

    assert(File.exist?("foo/test/compiler/test_jrubyc.class"))
  end
  
  def test_require
    $compile_test = false
    File.open("test_file1.rb", "w") {|file| file.write("$compile_test = true")}
    
    JRubyCompiler::compile_argv(["test_file1.rb"])
    output = File.read(@tempfile.path)

    assert_equal(
      "Compiling test_file1.rb to class test_file1\n",
      output)

    assert_nothing_raised { require 'test_file1' }
    assert($compile_test)
  ensure
    File.delete("test_file1.rb") rescue nil
    File.delete("test_file1.class") rescue nil
  end
end
