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

=begin Neither of these tests seem to work running under rake. FIXME
  def test_basic
    begin
      JRuby::Compiler::compile_argv(["--verbose", __FILE__])
      output = File.read(@tempfile.path)

      assert_equal(
        "Compiling #{__FILE__}\n",
        output)
      
      class_file = __FILE__.gsub('.rb', '.class')

      assert(File.exist?(class_file))
    ensure
      File.delete(class_file) rescue nil
    end
  end

  def test_target
    tempdir = File.dirname(@tempfile.path)
    JRuby::Compiler::compile_argv(["--verbose", "-t", tempdir, __FILE__])
    output = File.read(@tempfile.path)

    assert_equal(
      "Compiling #{__FILE__}\n",
      output)

    assert(File.exist?(tempdir + "/test/compiler/test_jrubyc.class"))
    FileUtils.rm_rf(tempdir + "/test/compiler/test_jrubyc.class")
  end
=end

  def test_bad_target
    begin
      JRuby::Compiler::compile_argv(["--verbose", "-t", "does_not_exist", __FILE__])
    rescue Exception => e
    end

    assert(e)
    assert_equal(
      "Target dir not found: does_not_exist",
      e.message)
  end
  
  def test_require
    $compile_test = false
    File.open("test_file1.rb", "w") {|file| file.write("$compile_test = true")}
    
    JRuby::Compiler::compile_argv(["--verbose", "test_file1.rb"])
    output = File.read(@tempfile.path)

    assert_equal(
      "Compiling test_file1.rb\n",
      output)

    assert_nothing_raised do
      require 'test_file1'
    end
    assert($compile_test)
  ensure
    File.delete("test_file1.rb") rescue nil
    File.delete("test_file1.class") rescue nil
  end
end
