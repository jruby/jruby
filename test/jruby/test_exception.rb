require 'test/unit'

class TestException < Test::Unit::TestCase

  def raise_an_error
    raise "an error"
  end

  def rescue_an_error
    begin
      raise_an_error
    rescue
      raise
    end
  end

  def test_good_stack_trace
    begin
      rescue_an_error
    rescue RuntimeError => e
      assert_match(/raise_an_error/, e.backtrace[0])
    end
  end

  def raise_circular_cause
    begin
      raise "error 1"
    rescue => e1
      raise "error 2" rescue e2 = $!
      raise e1, cause: e2
    end
  end

  def test_circular_cause_handle
    begin
      raise_circular_cause
    rescue => e
      assert_match(/raise_circular_cause/, e.backtrace[0])
      assert_equal("circular causes", e.message)
      assert_equal("error 1", e.cause.message)
    end
  end
end
