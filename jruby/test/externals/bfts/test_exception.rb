require 'test/unit' if $0 == __FILE__
require 'rubicon_testcase'

class TestException < Test::Unit::TestCase

  MSG = "duck"

  def test_s_exception
    e = Exception.exception
    assert_equal(Exception, e.class)

    e = Exception.exception(MSG)
    assert_equal(MSG, e.message)
  end

  def test_backtrace
    assert_nil(Exception.exception.backtrace)
    begin
      line=__LINE__; file=__FILE__; raise MSG
    rescue RuntimeError => detail
      assert_equal(RuntimeError, detail.class)
      assert_equal(MSG, detail.message)
      expected = "#{file}:#{line}:in `test_backtrace'"
      assert_equal(expected, detail.backtrace[0])
    end
  end

  def test_exception
    e = IOError.new
    assert_equal(IOError, e.class)
    assert_equal(IOError, e.exception.class)
    assert_equal(e,       e.exception)

    e = IOError.new
    e1 = e.exception(MSG)
    assert_equal(IOError, e1.class)
    assert_equal(MSG,     e1.message)
  end

  def test_message
    e = IOError.new(MSG)
    assert_equal(MSG, e.message)
  end

  def test_set_backtrace
    e = IOError.new
    a = %w( here there everywhere )
    assert_equal(a, e.set_backtrace(a))
    assert_equal(a, e.backtrace)
  end

  # FIX: this is retardedly complex
  # exercise bug in Exception#set_backtrace, see [ruby-talk:96273].
  class Problem # helper class for #test_set_backtrace2
    STACK = %w(a:0:A b:1:B c:2:C)
    def self.mk_problem
      raise IOError, "got nuked"
    rescue IOError => e
      error = IOError.new("got nuked")
      error.set_backtrace(STACK)
      raise error
    end
    def self.mk_exception
      begin
        self.mk_problem
        raise "should not happen"
      rescue IOError => e
        return e
      end
    end
  end
  def test_set_backtrace2
    e = Problem.mk_exception
    assert_equal("got nuked", e.message)
    # this is how set_backtrace is suppose to work
    assert_equal(Problem::STACK, e.backtrace)
  end
end
