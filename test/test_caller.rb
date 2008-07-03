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
end
