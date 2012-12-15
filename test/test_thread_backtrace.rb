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

    # These traces were modified during the new mixed-mode backtrace work
    # to match the RubyProc calls at the top of a new Thread's stack.
    if $0 == __FILE__
      expected = [
                  /test\/test_thread_backtrace\.rb:7:in `test_simple_backtrace'/,
                  /org\/jruby\/RubyProc\.java:[0-9]+:in `call'/,
                  /org\/jruby\/RubyProc\.java:[0-9]+:in `call'/]
    else
      expected = [
                  /\/test\/test_thread_backtrace\.rb:7:in `test_simple_backtrace'/,
                  /org\/jruby\/RubyProc\.java:[0-9]+:in `call'/,
                  /org\/jruby\/RubyProc\.java:[0-9]+:in `call'/]
    end

    expected.each_with_index do |pattern, index|
      assert pattern =~ backtrace[index],
          "mismatched traces: #{backtrace[index].inspect} did not match #{pattern.inspect}"
    end
  end
end
