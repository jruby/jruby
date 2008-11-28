require 'test/unit'

class TestNoStackTraceStomp < Test::Unit::TestCase
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
      assert_match /`raise_an_error'/, e.backtrace[0]
    end
  end
end
