require 'test/unit'

class TestCaller < Test::Unit::TestCase
  def foo
    eval "caller(0)"
  end

  def test_evaled_caller_has_full_trace
    trace = foo

    # simple test, make sure the trace is more than one entry
    assert(trace.length > 1)
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
