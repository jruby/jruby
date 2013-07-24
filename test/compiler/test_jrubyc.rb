require 'test/unit'
require 'stringio'
require 'tempfile'
require 'fileutils'
require 'java'
require 'jruby/jrubyc'

class TestJrubyc < Test::Unit::TestCase
  def setup
    @tempfile_stdout = Tempfile.open("test_jrubyc_stdout")
    @old_stdout = $stdout.dup
    $stdout.reopen @tempfile_stdout
    $stdout.sync = true

    @tempfile_stderr = Tempfile.open("test_jrubyc_stderr")
    @old_stderr = $stderr.dup
    $stderr.reopen @tempfile_stderr
    $stderr.sync = true
  end

  def teardown
    FileUtils.rm_rf(["foo", "ruby"])
    $stdout.reopen(@old_stdout)
    $stderr.reopen(@old_stderr)
  end

=begin Neither of these tests seem to work running under rake. FIXME
  def test_basic
    begin
      JRuby::Compiler::compile_argv(["--verbose", __FILE__])
      output = File.read(@tempfile_stdout.path)

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
    tempdir = File.dirname(@tempfile_stdout.path)
    JRuby::Compiler::compile_argv(["--verbose", "-t", tempdir, __FILE__])
    output = File.read(@tempfile_stdout.path)

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
    output = File.read(@tempfile_stdout.path)

    assert_equal(
      "Compiling test_file1.rb\n",
      output)

    assert_nothing_raised { require 'test_file1' }
    assert($compile_test)
  ensure
    File.delete("test_file1.rb") rescue nil
    File.delete("test_file1.class") rescue nil
  end

  def test_signature_with_arg_named_result
    $compile_test = false
    File.open("test_file2.rb", "w") {|file| file.write(<<-RUBY
      class C
        java_signature 'public int f(int result)'
        def f(arg)
          $compile_test = true
        end
      end

      C.new.f(0)
    RUBY
    )}

    JRuby::Compiler::compile_argv(["--verbose", "--java", "--javac", "test_file2.rb"])
    output = File.read(@tempfile_stderr.path)

    assert_equal("", output)

    assert_nothing_raised { require 'test_file2' }
    assert($compile_test)
  ensure
    File.delete("test_file2.rb") rescue nil
    File.delete("C.java") rescue nil
    File.delete("C.class") rescue nil
  end
end
