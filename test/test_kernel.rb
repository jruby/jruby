require 'test/unit'
require 'rbconfig'
require 'test/test_helper'
require 'pathname'

class TestKernel < Test::Unit::TestCase
  include TestHelper

  def log(msg)
    $stderr.puts msg if $VERBOSE
  end

  TESTAPP_DIR = File.expand_path(File.join(File.dirname(__FILE__), 'testapp'))

  if (WINDOWS)
    # the testapp.exe exists for sure, it is pre-built
    TESTAPP_EXISTS = true
  else
    Dir.chdir(TESTAPP_DIR) {
      file = File.join(TESTAPP_DIR, 'testapp')
      `gcc testapp.c -o testapp` unless (File.exist?(file))
       TESTAPP_EXISTS = File.exist?(file)
    }
  end

  TESTAPP_REL = File.join(File.join(File.dirname(__FILE__), 'testapp'), 'testapp')
  TESTAPP_REL_NONORM = File.join(File.join(File.join(File.join(File.dirname(__FILE__), 'testapp'), '..'), 'testapp'), 'testapp')
  TESTAPP = File.expand_path(TESTAPP_REL)
  TESTAPP_NONORM = File.join(File.join(File.join(TESTAPP_DIR, '..'), 'testapp'), 'testapp')

  # TODO: path with spaces!
  TESTAPPS = [] << TESTAPP_REL << TESTAPP << TESTAPP_REL_NONORM << TESTAPP_NONORM

  if WINDOWS
    TESTAPPS << TESTAPP_REL + ".exe"
    TESTAPPS << (TESTAPP_REL + ".exe").gsub('/', '\\')
    TESTAPPS << TESTAPP_REL + ".bat"
    TESTAPPS << (TESTAPP_REL + ".bat").gsub('/', '\\')
    TESTAPPS << TESTAPP + ".exe"
    TESTAPPS << (TESTAPP + ".exe").gsub('/', '\\')
    TESTAPPS << TESTAPP + ".bat"
    TESTAPPS << (TESTAPP + ".bat").gsub('/', '\\')
  end

  def test_stuff_seemingly_out_of_context
    assert !Kernel.eval("defined? some_unknown_variable")
    assert_equal nil, true && defined?(unknownConstant)

    # JRUBY-117 - to_a should be public
    unless RUBY_VERSION =~ /1\.9/
      assert_equal ["to_a"], Object.public_instance_methods.grep("to_a")
    end

    # JRUBY-117 - remove_instance_variable should be private
    if RUBY_VERSION =~ /1\.9/
      assert_equal [:remove_instance_variable], Object.private_instance_methods.grep(:remove_instance_variable)
    else
      assert_equal ["remove_instance_variable"], Object.private_instance_methods.grep("remove_instance_variable")
    end
  end


  def test_Array_should_return_empty_array_for_nil
    assert_equal [], Kernel.Array(nil)
  end

  class ToAryReturnsAnArray; def to_ary() [1] end; end
  class ToAryReturnsAnInteger; def to_ary() 2 end; end

  def test_Array_should_use_to_ary_and_make_sure_that_it_returns_either_array_or_nil
    assert_equal [1], Kernel.Array(ToAryReturnsAnArray.new)
    assert_raises(TypeError) { Kernel.Array(ToAryReturnsAnInteger.new) }    
  end

  class BothToAryAndToADefined; def to_ary() [3] end; def to_a() raise "to_a() should not be called" end; end
  class OnlyToADefined; def to_a() [4] end; end
  def test_Array_should_fallback_on_to_a_if_to_ary_is_not_defined
    assert_equal [3], Kernel.Array(BothToAryAndToADefined.new)
    assert_equal [4], Kernel.Array(OnlyToADefined.new)
  end

  class NeitherToANorToAryDefined; end
  def test_Array_should_return_array_containing_argument_if_the_argument_has_neither_to_ary_nor_to_a
    assert_equal [1], Kernel.Array(1)
    assert_equal [:foo], Kernel.Array(:foo)
    obj = NeitherToANorToAryDefined.new 
    assert_equal [obj], Kernel.Array(obj)
  end

  class ToAryReturnsNil; def to_ary() nil end; end
  def test_Array_should_return_array_containing_arg_if_to_ary_returns_nil
    obj = ToAryReturnsNil.new
    assert_equal [obj], Kernel.Array(obj)
  end

  def test_Integer
    assert_equal 10, Kernel.Integer("0xA")
    assert_equal 8, Kernel.Integer("010")
    assert_equal 2, Kernel.Integer("0b10")
    assert_raises(ArgumentError) { Kernel.Integer("abc") }
    assert_raises(ArgumentError) { Kernel.Integer("x10") }
    assert_raises(ArgumentError) { Kernel.Integer("xxxx10000000000000000000000000000000000000000000000000000") }
  end

  def test_Float
    assert_equal 1.0, Kernel.Float("1")
    assert_equal 10.0, Kernel.Float("1e1")
    assert_raises(ArgumentError) { Kernel.Float("abc") }
    assert_raises(ArgumentError) { Kernel.Float("x10") }
    assert_raises(ArgumentError) { Kernel.Float("xxxx10000000000000000000000000000000000000000000000000000") }
  end

#  String
#  URI
#  `
#  abort
#  at_exit
#  binding
#  block_given?
  class CheckBlockGiven; def self.go() block_given? end; end
  def test_iterator?
    assert !(Kernel.block_given?)
    assert(CheckBlockGiven.go { true })
    assert(!CheckBlockGiven.go)
    assert(!CheckBlockGiven.go(&Proc.new))
  end

#  callcc
#  caller

  def test_catch_throw
    been_where_it_shouldnt = false

    catch :fred do throw :fred; fail("shouldn't get here") end
    assert(!been_where_it_shouldnt)

    if RUBY_VERSION =~ /1\.9/
      ex = ArgumentError
    else
      ex = NameError
    end
    assert_raises (ex) do
      catch :fred do throw :wilma end
    end
  end

  def test_throw_should_bubble_up_to_the_right_catch
    been_at_fred2 = false
    catch :fred1 do
      catch :fred2 do
        catch :fred3 do
          catch :fred4 do
            throw :fred2
            fail("should have jumped to fred2 catch")
          end
          fail("should have jumped to fred2 catch")
        end
      end
      been_at_fred2 = true
    end
    assert been_at_fred2 
  end

  def test_invalid_throw_after_inner_catch_should_unwind_the_stack_all_the_way_to_the_top
    been_at_fred1 = false
    been_at_fred2 = false
    if RUBY_VERSION =~ /1\.9/
      ex = ArgumentError
    else
      ex = NameError
    end
    assert_raises(ex) do
      catch :fred1 do
        catch :fred2 do
          catch :fred3 do
            throw :fred2
            test_fail("should have jumped to after fred2 catch")
          end
          test_fail("should have jumped to after fred2 catch")
        end
        been_at_fred2 = true
        throw :wilma
      end
      been_at_fred1 = true
    end
    assert been_at_fred2
    assert !been_at_fred1
  end

  def throw_outside_of_any_block_shoudl_raise_an_error
    assert_raises (NameError) { throw :wilma }
  end

  def test_catch_stack_should_be_cleaned_up
    if RUBY_VERSION =~ /1\.9/
      ex = ArgumentError
    else
      ex = NameError
    end
    assert_raises(ex) do
      catch :fred1 do
        catch :fred2 do
          catch :fred3 do
          end
        end
      end
      throw :fred2
      fail "catch stack should have been cleaned up"
    end
  end
  
#  chomp
#  chomp!

  # JRUBY-2527
  unless RUBY_VERSION =~ /1\.9/
    def test_chomp_no_block_doesnt_break
      $_ = "test"
      assert_equal("test", chomp)
      assert_equal("te", chomp("st"))

      $_ = "test"
      chomp!
      assert_equal("test", $_)

      chomp!("st")
      assert_equal("te", $_)
    end
  end

#  chop
#  chop!


  #  eval
  def test_eval_should_use_local_variable_defined_in_parent_scope
    x = 1
    assert_equal 1, Kernel.eval('x')
    Kernel.eval("x = 2")
    assert_equal 2, x
  end

  def test_eval_should_not_bring_local_variables_defined_in_its_input_to_parent_scope
    existing_variable = 0
    another_variable = 1
    assert !(defined? new_variable)

    Kernel.eval("new_variable = another_variable = existing_variable")

    assert_equal 0, existing_variable 
    assert_equal 0, another_variable 
    assert !(defined? new_variable)
  end

#  exec
  # should not really work, since there is no fork

#  exit
#  exit!
#  fail
#  fork

  def test_format
    assert_equal "Hello, world", Kernel.format("Hello, %s", "world") 
    assert_raises(TypeError) { Kernel.format("%01.3f", nil) }
  end

#  getc
#  gets
#  global_variables
#  gsub
#  gsub!

  class CheckIterator; def self.go() iterator? end; end
  def test_iterator?
    assert !(Kernel.iterator?)
    assert(CheckIterator.go { true })
    assert(!CheckIterator.go)
  end

#  lambda
#  load
  class ToStrPointToRequireTarget
    attr_accessor :target_required
    def to_str() "#{File.dirname(__FILE__)}/require_target.rb" end
  end

  def test_load_should_call_to_str_on_arg
    $target_required = false
    Kernel.load ToStrPointToRequireTarget.new
    assert $target_required

    assert_raises(TypeError) { Kernel.load Object.new }
  end

  def test_shall_load_from_load_path
    tmp = ENV["TEMP"] || ENV["TMP"] || ENV["TMPDIR"] || "/tmp"
    Dir.chdir(tmp) do
      load_path_save = $LOAD_PATH.dup
      begin
        File.open(File.expand_path('.') +'/file_to_be_loaded','w' ) do |f|
          f.puts "raise"
        end
        $LOAD_PATH.delete_if{|dir| dir=='.'}
        assert_raise(LoadError) {
          load 'file_to_be_loaded'
        }
      ensure
        File.delete(File.expand_path('.') +'/file_to_be_loaded')
        $LOAD_PATH.clear
        $LOAD_PATH.concat(load_path_save)
      end
    end
  end

  def test_local_variables
    if RbConfig::CONFIG['ruby_install_name'] == 'jruby'
      if RUBY_VERSION =~ /1\.9/
        a = lambda do
          assert_equal [:a, :b], local_variables
          b = 1
          assert_equal [:a, :b], local_variables
        end
      else
        a = lambda do
          assert_equal %w(a b), local_variables
          b = 1
          assert_equal %w(a b), local_variables
        end
      end
      a.call
    else
      # This behaves like MRI 1.9, and fails on 1.8, so, skip it
    end
  end

#  loop
#  method_missing
#  open
#  p
#  print

  def test_printf_should_raise_argument_error_when_asked_to_format_string_as_a_number
    assert_raises(ArgumentError) { Kernel.printf "%d", 's' }
  end

#  proc
#  putc
#  puts

  def test_raise
    assert_raises(RuntimeError) { Kernel.raise }
    assert_raises(StandardError) { Kernel.raise(StandardError) }
    assert_raises(TypeError) { Kernel.raise(TypeError.new) }

    begin
      Kernel.raise(StandardError, 'oops')
      fail "shouldn't get here!"
    rescue => e1
      assert e1.is_a?(StandardError)
      assert_equal 'oops', e1.message
    end

    begin
      Kernel.raise('oops')
      fail "shouldn't get here!"
    rescue => e2
      assert e2.is_a?(StandardError)
      assert_equal 'oops', e2.message
    end
  end

  # JRUBY-2696
  def test_raise_in_debug_mode
    require 'stringio'
    old_debug, $DEBUG = $DEBUG, true

    # by Exception Class
    $stderr = StringIO.new
    raise StandardError rescue nil
    tobe = "Exception `StandardError' at #{__FILE__}:#{__LINE__ - 1} - StandardError"
    assert_equal(tobe, $stderr.string.split("\n")[0])

    # by String
    $stderr.reopen
    raise "TEST_ME" rescue nil
    tobe = "Exception `RuntimeError' at #{__FILE__}:#{__LINE__ - 1} - TEST_ME"
    assert_equal(tobe, $stderr.string.split("\n")[0])

    # by Exception
    $stderr.reopen
    raise RuntimeError.new("TEST_ME") rescue nil
    tobe = "Exception `RuntimeError' at #{__FILE__}:#{__LINE__ - 1} - TEST_ME"
    assert_equal(tobe, $stderr.string.split("\n")[0])

    # by re-raise
    $stderr.reopen
    raise "TEST_ME" rescue raise rescue nil
    tobe = "Exception `RuntimeError' at #{__FILE__}:#{__LINE__ - 1} - TEST_ME"
    traces = $stderr.string.split("\n")
    assert_equal(tobe, traces[0])
    assert_equal(tobe, traces[1])
  ensure
    $DEBUG = old_debug
    $stderr = STDERR
  end

#  rand
#  readline
#  readlines
#  scan
#  select
#  set_trace_func


  def test_sleep
    assert_raises(ArgumentError) { sleep(-10) }
    # FIXME: below is true for MRI, but not for JRuby 
    # assert_raises(TypeError) { sleep "foo" }
    assert_equal 0, sleep(0)
    t1 = Time.now
    sleep(0.1)
    t2 = Time.now
    # this should cover systems with 10 msec clock resolution
    assert t2 >= t1 + 0.08
  end

  def test_sprintf
    assert_equal 'Hello', Kernel.sprintf('Hello')
  end

  def test_sprintf_with_object
    obj = Object.new
    def obj.to_int() 4 end
    assert_equal '4', Kernel.sprintf('%d', obj)
  end

  # JRUBY-4802
  def test_sprintf_float
    assert_equal "0.00000", Kernel.sprintf("%.5f", 0.000004)
    assert_equal "0.00001", Kernel.sprintf("%.5f", 0.000005)
    assert_equal "0.00001", Kernel.sprintf("%.5f", 0.000006)
  end

  def test_srand
    Kernel.srand
    Kernel.srand(0)
    if RUBY_VERSION =~ /1\.9/
      assert_raises(TypeError) { Kernel.srand(:foo) }
    else
      Kernel.srand(:foo)
    end
    assert_raises(TypeError) { Kernel.srand([:foo]) }
  end

#  sub
#  sub!
#  syscall

  def test_system
    assert !system('non_existant_command')
    # TODO: test for other OSes (Windows, for example, wouldn't know what to do with /dev/null)
    if RbConfig::CONFIG['target_os'] == 'linux'
      assert_equal true, system('echo > /dev/null')
    end
  end

  def test_system_empty
    assert !system('')
  end

  unless RUBY_VERSION =~ /1\.9/ # FIXME figure out why this doesn't pass in 1.9 mode
    def test_system_existing
      quiet do
        if (WINDOWS)
          res = system('cd')
        else
          res = system('pwd')
        end
        assert_equal true, res
      end
    end
  end

  unless RUBY_VERSION =~ /1\.9/ # FIXME figure out why this doesn't pass in 1.9 mode
    def test_system_existing_with_leading_space
      quiet do
        if (WINDOWS)
          res = system(' cd')
        else
          res = system(' pwd')
        end
        assert_equal true, res
      end
    end
  end

  unless RUBY_VERSION =~ /1\.9/ # FIXME figure out why this doesn't pass in 1.9 mode
    def test_system_existing_with_trailing_space
      quiet do
        if (WINDOWS)
          res = system('cd ')
        else
          res = system('pwd ')
        end
        assert_equal true, res
      end
    end
  end

  def test_system_non_existing
    res =  system('program-that-doesnt-exist-for-sure')
    assert !res
  end

  unless RUBY_VERSION =~ /1\.9/ # FIXME figure out why this doesn't pass in 1.9 mode
    def test_system_existing_with_arg
      if (WINDOWS)
        res = system('cd .')
      else
        res = system('pwd . 2>&1 > /dev/null')
      end
      assert_equal true, res
    end
  end

  def test_system_non_existing_with_arg
    res = system('program-that-doesnt-exist-for-sure .')
    assert !res
  end

  def test_system_non_existing_with_args
    res = system('program-that-doesnt-exist-for-sure','arg1','arg2')
    assert !res
  end

  def test_exec_empty
      assert_raise(Errno::ENOENT) {
        exec("")
      }
  end

  def test_exec_non_existing
    assert_raise(Errno::ENOENT) {
      exec("program-that-doesnt-exist-for-sure")
    }
  end

  def test_exec_non_existing_with_args
    assert_raise(Errno::ENOENT) {
      exec("program-that-doesnt-exist-for-sure", "arg1", "arg2")
    }
  end

  # JRUBY-4834
  def test_backquote_with_changed_path
    orig_env = ENV['PATH']

    # Append a directory where testapp resides to the PATH
    paths = (ENV["PATH"] || "").split(File::PATH_SEPARATOR)
    paths.unshift TESTAPP_DIR
    ENV["PATH"] = paths.uniq.join(File::PATH_SEPARATOR)

    res = `testapp`.chomp
    assert_equal("NO_ARGS", res)
  ensure
    ENV['PATH'] = orig_env
  end

  # JRUBY-4127
  def test_backquote_with_quotes
    if (WINDOWS)
      result = `"#{TESTAPP_NONORM}" #{Dir.pwd}`.strip
    else
      result = `"pwd" .`.strip
    end
    expected = Dir.pwd
    assert_equal(expected, result)
  end

  def test_backquote1
    if (WINDOWS)
      result = `cmd /c cd`.strip.gsub('\\', '/').downcase
      expected = Dir.pwd.downcase
    else
      result = `sh -c pwd`.strip
      expected = Dir.pwd
    end
    assert_equal(expected, result)
  end

  def test_backquote1_1
    if (WINDOWS)
      result = `cmd.exe /c cd`.strip.gsub('\\', '/').downcase
      expected = Dir.pwd.downcase
    else
      result = `pwd`.strip
      expected = Dir.pwd
    end
    assert_equal(expected, result)
  end

  def test_backquote2
    TESTAPPS.each { |app|
      if (app =~ /\/.*\.bat/ && Pathname.new(app).relative?)
        # MRI can't launch relative BAT files with / in their paths
        log "-- skipping #{app}"
        next
      end
      log "testing #{app}"

      result = `#{app}`.strip
      assert_equal('NO_ARGS', result, "Can't properly launch '#{app}'")
    }
  end

  def test_backquote2_1
    TESTAPPS.each { |app|
      if (app =~ /\/.*\.bat/ && Pathname.new(app).relative?)
        # MRI can't launch relative BAT files with / in their paths
        log "-- skipping #{app}"
        next
      end
      log "testing #{app}"

      result = `#{app} one`.strip
      assert_equal('one', result, "Can't properly launch '#{app}'")
    }
  end

  def test_backquote3
    TESTAPPS.each { |app|
      if (app =~ /\// && Pathname.new(app).relative? && WINDOWS)
        log "-- skipping #{app}"
        next
      end

      if (TESTAPP_DIR =~ /\s/) # spaces in paths, quote!
        app = '"' + app + '"'
      end

      log "testing #{app}"

      result = `#{app} 2>&1`.strip
      assert_equal("NO_ARGS", result, "Can't properly launch '#{app}'")
    }
  end

  def test_backquote3_1
    TESTAPPS.each { |app|
      if (app =~ /\// && Pathname.new(app).relative? && WINDOWS)
        log "-- skipping #{app}"
        next
      end

      if (TESTAPP_DIR =~ /\s/) # spaces in paths, quote!
        app = '"' + app + '"'
      end

      log "testing #{app}"

      result = `#{app} one 2>&1`.strip
      assert_equal("one", result, "Can't properly launch '#{app}'")
    }
  end

  def test_backquote3_2
    TESTAPPS.each { |app|
      if (app =~ /\// && Pathname.new(app).relative? && WINDOWS)
        log "-- skipping #{app}"
        next
      end

      if (TESTAPP_DIR =~ /\s/) # spaces in paths, quote!
        app = '"' + app + '"'
      end

      log "testing #{app}"

      result = `#{app} one two three 2>&1`.strip.split(/[\r\n]+/)
      assert_equal(%w[one two three], result, "Can't properly launch '#{app}'")

      # TODO: \r\n vs \n
      # result = `#{app} one two three 2>&1`.strip
      # assert_equal("one\ntwo\nthree", result, "Can't properly launch '#{app}'")
    }
  end

  def test_backquote4
    TESTAPPS.each { |app|
      if (app =~ /\/.*\.bat/ && Pathname.new(app).relative?)
        # MRI can't launch relative BAT files with / in their paths
        log "-- skipping #{app}"
        next
      end
      log "testing #{app}"

      result = `#{app} "" two`.split(/[\r\n]+/)
      assert_equal(["", "two"], result, "Can't properly launch '#{app}'")

      result = `#{app} ""`.chomp
      assert_equal('', result, "Can't properly launch '#{app}'")

      result = `#{app} "   "`.chomp
      assert_equal('   ', result, "Can't properly launch '#{app}'")

      result = `#{app} a""`.chomp
      assert_equal('a', result, "Can't properly launch '#{app}'")

      result = `#{app} a" "`.chomp
      assert_equal('a ', result, "Can't properly launch '#{app}'")

      result = `#{app} ""b`.chomp
      assert_equal('b', result, "Can't properly launch '#{app}'")

      result = `#{app} " "b`.chomp
      assert_equal(' b', result, "Can't properly launch '#{app}'")

      result = `#{app} a""b`.chomp
      assert_equal('ab', result, "Can't properly launch '#{app}'")

      result = `#{app} a"   "b`.chomp
      assert_equal('a   b', result, "Can't properly launch '#{app}'")

      result = `#{app} a"   "b"  "c`.chomp
      assert_equal('a   b  c', result, "Can't properly launch '#{app}'")

      result = `#{app} a"   "b""c`.chomp
      assert_equal('a   bc', result, "Can't properly launch '#{app}'")
    }
  end

  def test_backquote4_1
    TESTAPPS.each { |app|
      if (app =~ /\// && Pathname.new(app).relative? && WINDOWS)
        log "-- skipping #{app}"
        next
      end
      log "testing #{app}"
      
      if (TESTAPP_DIR =~ /\s/) # spaces in paths, quote!
        app = '"' + app + '"'
      end

      result = `#{app} "   " 2>&1`.chomp
      assert_equal('   ', result, "Can't properly launch '#{app}'")

      result = `#{app} a"" 2>&1`.strip
      assert_equal('a', result, "Can't properly launch '#{app}'")

      result = `#{app} ""b 2>&1`.strip
      assert_equal('b', result, "Can't properly launch '#{app}'")

      result = `#{app} a""b 2>&1`.strip
      assert_equal('ab', result, "Can't properly launch '#{app}'")

      result = `#{app} a"   "b 2>&1`.strip
      assert_equal('a   b', result, "Can't properly launch '#{app}'")
    }
  end

  def test_backquote_with_executable_in_cwd
    Dir.chdir(TESTAPP_DIR) do
      result = `./testapp one`
      assert_equal 0, $?.exitstatus
      assert_equal "one", result.rstrip
    end
  end

  if (WINDOWS)
    def test_backquote_with_executable_in_cwd_2
      Dir.chdir(TESTAPP_DIR) do
        result = `testapp one`
        assert_equal 0, $?.exitstatus
        assert_equal "one", result.rstrip
      end
    end
  end

  # JRUBY-4518
  if (WINDOWS)
    def test_backquote_with_CRLF
      assert_no_match(/\r/, `cd`)
      assert_equal 0, $?.exitstatus
    end
  end

  unless RUBY_VERSION =~ /1\.9/ # FIXME figure out why this doesn't pass in 1.9 mode
    def test_system_with_executable_in_cwd
      Dir.chdir(TESTAPP_DIR) do
        result = nil
        quiet do
          result = system("./testapp one")
        end
        assert_equal 0, $?.exitstatus
        assert result
      end
    end
  end

  if (WINDOWS)
    def test_system_with_executable_in_cwd_2
      Dir.chdir(TESTAPP_DIR) do
        result = nil
        quiet do
          result = system("testapp one")
        end
        assert_equal 0, $?.exitstatus
        assert result
      end
    end
  end

  unless RUBY_VERSION =~ /1\.9/
    def test_test
      assert "Test file existence", test(?f, "README")
      assert "Test file non-existence", !test(?f, "READMEaewertsert45t4w5tgrsfdgrf")
      # Make sure that absolute paths work for testing
      assert "Test should handle absolute paths", test(?f, File.expand_path("README"))
    end
  end

  # JRUBY-4348
  def test_exec_rubyopt
    old = ENV['RUBYOPT']
    ENV['RUBYOPT'] = "-v"
    result =  `bin/jruby -e "a=1"`
    assert_equal 0, $?.exitstatus
    assert_match /ruby/i, result
  ensure
    ENV['RUBYOPT'] = old
  end

  # JRUBY-5431
  def test_exit_bang
    `bin/jruby -e "exit!"`
    assert_equal 1, $?.exitstatus
    `bin/jruby -e "exit!(true)"`
    assert_equal 0, $?.exitstatus
    `bin/jruby -e "exit!(false)"`
    assert_equal 1, $?.exitstatus
  end

#  test
#  trace_var
#  trap
#  untrace_var
#  warn

end
