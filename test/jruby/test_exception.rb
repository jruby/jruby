require 'test/unit'

class TestException < Test::Unit::TestCase

  class AnError < RuntimeError; end

  def raise_an_error
    raise AnError
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
      assert_match(/`raise_an_error'/, e.backtrace[0])
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
      assert_match(/`raise_circular_cause'/, e.backtrace[0])
      assert_equal(e.message, "error 1")
      assert_equal(e.cause.message, "error 2")
      assert_equal(e.cause.cause.message, "error 1")
    end
  end

  def test_exception_no_rescue_ensure
    begin
      raise_no_rescue_and_ensure!
    rescue NotImplementedError
      # pass
    end
    assert_nil $!
  end

  def raise_no_rescue_and_ensure!
    begin
      raise NotImplementedError
    rescue
      fail 'not expected'
    ensure
      assert_instance_of NotImplementedError, $!
    end
    assert_nil $!
  end

  def test_exception_rescue(previous_error = nil)
    begin
      raise TypeError, "duck"
    rescue
      assert_equal "duck", $!.message
    ensure
      assert_same previous_error, $!
    end
    assert_same previous_error, $!

    begin
      raise 'foo'
    rescue TypeError
      fail 'should not rescue here' if $!
    rescue RuntimeError
      assert_instance_of RuntimeError, $!
    end
    assert_same previous_error, $!

    begin
      raise_an_error
    rescue AnError
      # pass
    end
    assert_same previous_error, $!
  end

  def test_exception_rescue_nested
    begin
      raise_no_rescue_and_ensure!
    rescue NotImplementedError => e
      test_exception_rescue(e)
      assert_instance_of NotImplementedError, $!
    end
    assert_nil $!

    begin
      raise_no_rescue_and_ensure!
    rescue NotImplementedError
      test_exception_rescue $!
      assert_instance_of NotImplementedError, $!
    ensure
      assert_nil $!
    end
    assert_nil $!

    begin
      begin
        raise_no_rescue_and_ensure!
      ensure
        prev = $!
        assert_instance_of NotImplementedError, $!
        test_exception_rescue(prev)
        assert_same prev, $!
      end
    rescue NotImplementedError
      # pass
    end
    assert_nil $!
  end

  def test_exception_rescue_java(previous_error = nil)
    begin
      raise java.lang.IllegalStateException.new('from java')
    rescue
      assert_equal "from java", $!.message
    ensure
      assert_same previous_error, $!
    end
    assert_same previous_error, $!

    begin
      java.util.ArrayList.new.get(1)
      raise 'foo'
    rescue RuntimeError
      fail 'should not rescue here' if $!
    rescue java.lang.RuntimeException
      assert_instance_of java.lang.IndexOutOfBoundsException, $!
    end
    assert_same previous_error, $!

    begin
      begin
        java.util.ArrayList.new.addAll(nil)
      ensure
        assert_instance_of java.lang.NullPointerException, $!
      end
    rescue java.lang.NullPointerException
      # pass
    end
    assert_same previous_error, $!
  end if defined? JRUBY_VERSION

end
