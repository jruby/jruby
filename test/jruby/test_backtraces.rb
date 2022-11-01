require 'java'
require 'test/unit'

# Unit and regression tests for backtraces.
# Note: The tests follow MRI 1.8.6 behavior.
# Behavior of MRI 1.9 is different:
# http://blade.nagaokaut.ac.jp/cgi-bin/scat.rb/ruby/ruby-core/15589
class TestBacktraces < Test::Unit::TestCase

  def test_simple_exception
    @offset = __LINE__
    raise RuntimeError.new("Test")
  rescue Exception => ex
    expectation = "+1:in `test_simple_exception'"
    assert_exception_backtrace(expectation, ex)
  end

  def test_native_java_backtrace
    org.jruby.test.TestHelper.throwTestHelperException
    fail 'did no raise exception'
  rescue NativeException => ex
    assert_equal '#<NativeException: org.jruby.test.TestHelper$TestHelperException: null>', ex.inspect
    assert_equal 'org.jruby.test.TestHelper$TestHelperException: null', ex.message
    assert_not_nil ex.cause
    assert_instance_of org.jruby.test.TestHelper::TestHelperException, ex.cause

    # starts with Java stack trace part :
    _throwTestHelperException = /org.jruby.test.TestHelper\.java:\d+:in `throwTestHelperException'/
    assert_match _throwTestHelperException, ex.backtrace[0]
    java_trace = ex.backtrace.find_all do |trace|
      trace =~ _throwTestHelperException
    end
    assert_equal 1, java_trace.length # only once!

    _test_native_java_backtrace = /test\/jruby\/test_backtraces.rb:\d+:in `test_native_java_backtrace'/

    ruby_trace = ex.backtrace.find_all do |trace|
      trace =~ _test_native_java_backtrace
    end
    assert_equal 1, ruby_trace.length # only once!

    backtrace = ex.backtrace.join("\r\n")
    unless backtrace.include?('test_native_java_backtrace')
      flunk("test_java_backtrace not in backtrace")
    end
  end

  def test_native_java_backtrace2
    sample_class = java.lang.Class.for_name('org.jruby.javasupport.test.name.Sample')
    constructor = sample_class.constructor(Java::int)
    constructor.new_instance 0
    begin
      constructor.new_instance(-1)
      fail 'did no raise exception'
    rescue NativeException => ex
      assert_equal 'java.lang.reflect.InvocationTargetException: param == -1', ex.message
      assert_equal '#<NativeException: java.lang.reflect.InvocationTargetException: param == -1>', ex.inspect
      assert_instance_of java.lang.reflect.InvocationTargetException, ex.cause
      # NOTE: backtrace will be messed in this case as long as there's filtering
      # clases org.jruby.javasupport.test.name.Sample "org.jruby.javasupport" prefix is considered internal
      # ex.backtrace.each { |b| puts "  #{b.inspect}" }
    end
  end

  def test_java_backtrace
    org.jruby.test.TestHelper.throwTestHelperException
    fail 'did no raise exception'
  rescue java.lang.Exception => ex
    # TestHelperException extends RuntimeException
    # assert_nil ex.message

    _throwTestHelperException = /org.jruby.test.TestHelper.throwTestHelperException/
    assert_match _throwTestHelperException, ex.backtrace[0]
    java_trace = ex.backtrace.find_all do |trace|
      trace =~ _throwTestHelperException
    end
    assert_equal 1, java_trace.length # only once!

    ruby_trace = ex.backtrace.find_all do |trace|
      trace.index('test_java_backtrace') && trace =~ /test\/jruby\/test_backtraces.rb:\d+/
    end
    assert_equal 1, ruby_trace.length # only once!
  end

  def test_simple_exception_recursive
    @offset = __LINE__
    def meth(n)
      raise RuntimeError.new("Test") if n == 10
      n += 1
      meth(n)
    end
    meth(0)
  rescue Exception => ex
    expectation = %q{
      +2:in `meth'
      +4:in `meth'
      +6:in `test_simple_exception_recursive'
    }
    assert_exception_backtrace(expectation, ex)
  end

  def test_native_exception_recursive
    @offset = __LINE__
    def meth(n)
      raise "hello".sub(/l/, 5) if n == 10
      n += 1
      meth(n)
    end
    meth(0)
  rescue Exception => ex
    expectation = %q{
      +2:in `sub'
      +2:in `meth'
      +4:in `meth'
      +6:in `test_native_exception_recursive'
    }
    assert_exception_backtrace(expectation, ex)
  end

  def test_exception_from_block
    @offset = __LINE__
    def foo
      yield
    end
    def bar
      yield
    end
    foo { bar { raise TypeError.new("HEH") } }
  rescue Exception => ex
    expectation = %q{
      +7:in `test_exception_from_block'
      +5:in `bar'
      +7:in `test_exception_from_block'
      +2:in `foo'
      +7:in `test_exception_from_block'
    }
    assert_exception_backtrace(expectation, ex)
  end

  def test_exception_from_for
    array = [1,2,3,4,5]
    @offset = __LINE__
    for e in array
      raise RuntimeError if e
    end
  rescue Exception => ex
    expectation = %q{
      +2:in `test_exception_from_for'
      +1:in `each'
      +1:in `test_exception_from_for'
    }
    assert_exception_backtrace(expectation, ex)
  end

  def test_exception_from_proc
    p = Proc.new {
      @offset = __LINE__
      raise StandardError.new
    }
    p.call
  rescue Exception => ex
    expectation = %q{
      +1:in `test_exception_from_proc'
      +3:in `call'
      +3:in `test_exception_from_proc'
    }
    assert_exception_backtrace(expectation, ex)
  end

  def test_exception_from_lambda
    l = lambda {
      @offset = __LINE__
      raise StandardError.new
    }
    l.call
  rescue Exception => ex
    expectation = %q{
      +1:in `test_exception_from_lambda'
      +3:in `call'
      +3:in `test_exception_from_lambda'
    }
    assert_exception_backtrace(expectation, ex)
  end

  def test_exception_from_array_plus
    @offset = __LINE__
    [1,2,3] + 5
  rescue Exception => ex
    expectation = %q{
      +1:in `+'
      +1:in `test_exception_from_array_plus'
    }
    assert_exception_backtrace(expectation, ex)
  end

  # JRUBY-2138
  def test_exception_from_string_plus
    @offset = __LINE__
    "hello" + nil
  rescue Exception => ex
    expectation = %q{
      +1:in `+'
      +1:in `test_exception_from_string_plus'
    }
    assert_exception_backtrace(expectation, ex)
  end

  def test_exception_from_string_sub
    @offset = __LINE__
    "hello".sub(/l/, 5)
  rescue Exception => ex
    expectation = %q{
      +1:in `sub'
      +1:in `test_exception_from_string_sub'
    }
    assert_exception_backtrace(expectation, ex)
  end

  def test_zero_devision_exception
    @offset = __LINE__
    1/0
  rescue Exception => ex
    expectation = %q{
      +1:in `/'
      +1:in `test_zero_devision_exception'
    }
    assert_exception_backtrace(expectation, ex)
  end

  def test_exeption_from_object_send
    @offset = __LINE__
    "hello".__send__(:sub, /l/, 5)
  rescue Exception => ex
    expectation = %q{
      +1:in `sub'
      +1:in `__send__'
      +1:in `test_exeption_from_object_send'
    }
    assert_exception_backtrace(expectation, ex)
  end

  def test_arity_exception
    @offset = __LINE__
    "hello".sub
  rescue Exception => ex
    expectation = "+1:in `sub'"
    assert_exception_backtrace(expectation, ex)
  end

  def test_exception_from_eval
    ex = get_exception {
      @offset = __LINE__
      eval("raise RuntimeError.new")
    }
    expectation = %Q{
      +1:in `test_exception_from_eval'
      #{__FILE__}:#{@get_exception_yield_line}:in `eval'
      +1:in `test_exception_from_eval'
      #{__FILE__}:#{@get_exception_yield_line}:in `get_exception'
    }
    assert_exception_backtrace(expectation, ex)
  end

  def test_exception_from_block_inside_eval
    ex = get_exception {
      @offset = __LINE__
      eval("def foo; yield; end; foo { raise RuntimeError.new }")
    }
    expectation = %Q{
      +1:in `test_exception_from_block_inside_eval'
      (eval):1:in `foo'
      (eval):1:in `test_exception_from_block_inside_eval'
      #{__FILE__}:#{@get_exception_yield_line}:in `eval'
      +1:in `test_exception_from_block_inside_eval'
      #{__FILE__}:#{@get_exception_yield_line}:in `get_exception'
    }
    assert_exception_backtrace(expectation, ex)
  end

  # JRUBY-2695
  def test_exception_from_thread_with_abort_on_exception_true
    require 'stringio'
    $stderr = StringIO.new

    Thread.abort_on_exception = true
    ex = get_exception {
      @offset = __LINE__
      t = Thread.new do
        raise RuntimeError.new "DUMMY_MSG"
      end
      sleep 3
      t.join
    }

    assert_equal(RuntimeError, ex.class)

    # This check is not fully MRI-compatible (MRI reports more frames),
    # but at list this is something.
    expectation = %Q{
      +2:in `test_exception_from_thread_with_abort_on_exception_true'
    }
    assert_exception_backtrace(expectation, ex)
  ensure
    Thread.abort_on_exception = false
    $stderr = STDERR
  end

  def test_throwing_runnable_backtrace # GH-3177
    fixnum_times_ = 'org.jruby.RubyFixnum.times(org/jruby/RubyFixnum.java:'
    backtrace = nil

    i = 0
    throwing = org.jruby.javasupport.test.ThrowingRunnable.new do
      1.times {
        begin
          throwing.doRun( (i += 1) > 0 )
        rescue java.lang.Exception
          assert e = $!.backtrace.find { |e2| e2.index('org.jruby.RubyFixnum.times') }
          assert_equal fixnum_times_, e[ 0...fixnum_times_.size ]
          backtrace = $!.backtrace.dup
          raise
        end
      }
    end

    begin
      throwing.doRun(false)
    rescue java.lang.Exception
      # puts $!.backtrace
      # second rewriting of the same exception :
      assert e = $!.backtrace.find { |e2| e2.index('org.jruby.RubyFixnum.times') }
      assert_equal fixnum_times_, e[ 0...fixnum_times_.size ]
      # NOTE back-trace gets duplicate .rb calls - seems not necessary to fix?!
      # assert_equal backtrace, $!.backtrace # expect the same back-trace
    else
      fail 'expected to throw a java.lang.Exception'
    end
  end

  private

  # Convenience method to obtain the exception,
  # and to print the stack trace, if needed.
  def get_exception(verbose = false)
    begin
      @get_exception_yield_line = __LINE__ + 1
      yield
    rescue Exception => ex
      puts ex.backtrace.join("\n") if verbose
      ex
    end
  end

  # Main verification method that performs actual checks
  # on the stacktraces.
  def assert_exception_backtrace(expectations, exception)
    backtrace = []
    expectations.strip.split("\n").each { |line|
      line.strip!

      # if line starts with +nnn, we prepend the current file and offset
      if line.match(/^\+(\d+)(:.*)/)
        flunk("@offset is not defined in the test case") unless @offset ||= nil
        # For JRuby, we soften this requirement, since native calls will
        # show their actual .java file and line, rather than the caller.
        #line = "#{__FILE__}:#{$1.to_i + @offset}#{$2}"
        line = /.*:#{$1.to_i + @offset}#{$2}/
      end
    }
    backtrace.each_with_index { |expected, idx|
      # Soften, per above comment
      #assert_equal(expected, exception.backtrace[idx])
      assert expected =~ exception.backtrace[idx]
    }
  end

end
