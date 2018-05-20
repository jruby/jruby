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
      expected = [ /test\/jruby\/test_thread_backtrace\.rb:7:in `block in test_simple_backtrace'/ ]
    else
      expected = [ /test\/jruby\/test_thread_backtrace\.rb:7:in `block in test_simple_backtrace'/ ]
    end

    puts "  " + backtrace.join("\n  ") if $VERBOSE

    expected.each_with_index do |pattern, index|
      assert pattern =~ backtrace[index],
          "mismatched traces: #{backtrace[index].inspect} did not match #{pattern.inspect}"
    end
  end
end
