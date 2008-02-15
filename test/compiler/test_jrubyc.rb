require 'test/unit'
require 'stringio'
require 'tempfile'
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
    $stdout.reopen(@old_stdout)
  end

  def test_basic
    begin
      JRubyCompiler::compile_argv([__FILE__])
      output = File.read(@tempfile.path)

      assert_equal(
        "Compiling #{__FILE__} to class ruby/test/compiler/test_jrubyc\n",
        output)

      assert(File.exist?("ruby/test/compiler/test_jrubyc.class"))
    ensure
      File.delete("ruby") rescue nil
    end
  end
  
  def test_target
    begin
      JRubyCompiler::compile_argv(["-t", File.dirname(@tempfile.path), __FILE__])
      output = File.read(@tempfile.path)

      assert_equal(
        "Compiling #{__FILE__} to class ruby/test/compiler/test_jrubyc\n",
        output)

      assert(File.exist?("ruby/test/compiler/test_jrubyc.class"))
    ensure
      File.delete("ruby") rescue nil
    end
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
    begin
      JRubyCompiler::compile_argv(["-p", "foo", __FILE__])
      output = File.read(@tempfile.path)

      assert_equal(
        "Compiling #{__FILE__} to class foo/test/compiler/test_jrubyc\n",
        output)

      assert(File.exist?("foo/test/compiler/test_jrubyc.class"))
    ensure
      File.delete("ruby") rescue nil
    end
  end
  
  def test_require
    $compile_test = false
    File.open("test_file1.rb", "w") {|file| file.write("$compile_test = true")}
    
    begin
      JRubyCompiler::compile_argv(["test_file1.rb"])
      output = File.read(@tempfile.path)
      
      assert_equal(
        "Compiling test_file1.rb to class ruby/test_file1\n",
        output)
      
      File.delete("test_file1.rb")
      
      assert_nothing_raised { require 'ruby/test_file1' }
      assert($compile_test)
    ensure
      File.delete("ruby") rescue nil
    end
  end
end
