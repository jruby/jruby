require 'test/unit'

class TestThreadBacktrace < Test::Unit::TestCase
  def test_simple_backtrace
    backtrace = Thread.new do
      begin
        raise
      rescue Exception => e
        e.backtrace
      end
    end.value

    if $0 == __FILE__
      expected = [
                  "test/test_thread_backtrace.rb:7:in `test_simple_backtrace'",
                  "test/test_thread_backtrace.rb:5:in `initialize'",
                  "test/test_thread_backtrace.rb:5:in `new'",
                  "test/test_thread_backtrace.rb:5:in `test_simple_backtrace'"]
    else
      expected = [
                  "./test/test_thread_backtrace.rb:7:in `test_simple_backtrace'",
                  "./test/test_thread_backtrace.rb:5:in `initialize'",
                  "./test/test_thread_backtrace.rb:5:in `new'",
                  "./test/test_thread_backtrace.rb:5:in `test_simple_backtrace'"]
    end

    assert_equal expected, backtrace[0..3]
  end
end
