require 'test/unit'
require 'stringio'
require 'tempfile'
require 'fileutils'

class TestJRubyc < Test::Unit::TestCase

  def setup; require 'jruby/jrubyc'

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
    FileUtils.rm_rf(["foo", "ruby"]) rescue nil

    $stdout.reopen(@old_stdout) if @old_stdout
    $stderr.reopen(@old_stderr) if @old_stderr

    $compile_test = nil; $encoding = nil
  end

  def test_basic
    JRuby::Compiler::compile_argv ["--verbose", __FILE__]

    output = File.read(@tempfile_stdout.path)
    assert_equal "Compiling #{__FILE__}\n", output

    class_file = __FILE__.sub('.rb', '.class')

    assert File.exist?(class_file)
  ensure
    ( class_file && FileUtils.rm_rf(class_file) ) rescue nil
  end

  def test_target
    tempdir = File.dirname(@tempfile_stdout.path)
    class_file = File.join(tempdir, __FILE__.sub('.rb', '.class'))

    JRuby::Compiler::compile_argv ["--verbose", "--target", tempdir, __FILE__]

    output = File.read(@tempfile_stdout.path)
    assert_equal "Compiling #{__FILE__}\n", output

    assert File.exist?(class_file)
  ensure
    ( class_file && FileUtils.rm_rf(class_file) ) rescue nil
  end

  def test_bad_target
    begin
      JRuby::Compiler::compile_argv(["--verbose", "-t", "does_not_exist", __FILE__])
      fail "expected #{__method__} to raise"
    rescue Exception => e
      assert_equal "Target dir not found: does_not_exist", e.message
    end
  end

  def test_require
    $compile_test = false
    File.open("test_file1.rb", "w") { |file| file.write("$compile_test = true") }

    JRuby::Compiler::compile_argv(["--verbose", "test_file1.rb"])
    output = File.read(@tempfile_stdout.path)

    assert_equal "Compiling test_file1.rb\n", output

    assert_nothing_raised { require 'test_file1' }
    assert($compile_test)
  ensure
    File.delete("test_file1.rb") rescue nil
    File.delete("test_file1.class") rescue nil
  end

  def test_unicode
    file = Tempfile.create("test_unicode")
    filename = file.path
    file.write("$encoding = 'jalapeño'.encoding")

    JRuby::Compiler::compile_argv(["--verbose", filename, "--dir", File.dirname(filename)])

    file.close

    assert_nothing_raised { load filename }
    assert_equal(Encoding::UTF_8, $encoding)
  ensure
    file.close rescue nil
  end

  # only filesystem installations of jruby can compile ruby to java
  if !(RbConfig::CONFIG['bindir'].match( /!\//) || RbConfig::CONFIG['bindir'].match( /:\//))

    def test_signature_with_arg_named_result
      $compile_test = false
      File.open("test_file2.rb", "w") { |file| file.write(<<-RUBY
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

  private

  def println(msg); Java::JavaLang::System.out.println(msg.to_s) end

end
