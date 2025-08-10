require 'test/unit'

class TestCaller < Test::Unit::TestCase

  def test_normal_caller
    trace0 = caller(0)
    assert_match(/test\/jruby\/test_caller\.rb\:6\:in 'test_normal_caller'/, trace0.first)
    assert(trace0.length > 1, "caller(0) is not > 1: #{trace0.inspect}")

    trace = caller
    assert_not_match(/test\/jruby\/test_caller\.rb\:16\:in 'test_normal_caller'/, trace[0])
    assert_equal trace0[1..-1], trace
  end

  def foo0; eval "caller(0)" end

  def test_evaled_caller_has_full_trace
    trace = foo0

    # puts trace.join("\n ")

    # simple test, make sure the trace is more than one entry
    assert(trace.length > 1, "caller(0) is not > 1: #{trace.inspect}")

    assert_match(/test\/jruby\/test_caller\.rb\:\d\d\:in 'eval'/, trace[1])
    assert_match(/test\/jruby\/test_caller\.rb\:\d\d\:in 'foo0'/, trace[2])
  end

  def test_jitted_caller_excludes_abstractscript
    eval 'def jitted_method(*args); caller(0); end'

    # call a number of times with args and block to ensure it
    # JITs and passes through AbstractScript.__file__
    100.times { jitted_method(1,2,3) {} }

    # check caller to ensure no AbstractScript.java shows up
    caller = jitted_method(1,2,3) {}
    assert caller.to_s !~ /AbstractScript/
  end

end
