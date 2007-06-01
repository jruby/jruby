require 'test/unit'


class TestContinuation < Test::Unit::TestCase

  # Helper methods

  def helper_method(cont)
    helper_method_one(cont)
    "bad"
  end

  def helper_method_one(cont)
    cont.call "OK"
    "worse"
  end

  # Tests proper

  def test_00_sanity
    callcc { |cont| assert_equal(Continuation, cont.class) }
  end

  def test_call_no_args
    result = callcc { |cont| cont.call }
    assert_equal(nil, result)
  end

  def test_call_one_args
    result = callcc { |cont| cont.call(99) }
    assert_equal(99, result)
  end

  def test_call_many_args
    expected = [ 'cat', 13, /a/ ]

    result = callcc { |cont| cont.call(*expected) }
    assert_equal(expected, result)
  end

  def test_remote_call
    result = callcc { |cont| 
      helper_method(cont)
      failt("never get here")
    }
    assert_equal("OK", result)
  end
end
