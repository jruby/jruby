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
      expected = [ /test\/jruby\/test_thread_backtrace\.rb:8:in 'block in test_simple_backtrace'/ ]
    else
      expected = [ /test\/jruby\/test_thread_backtrace\.rb:8:in 'block in test_simple_backtrace'/ ]
    end

    puts "  " + backtrace.join("\n  ") if $VERBOSE

    expected.each_with_index do |pattern, index|
      assert pattern =~ backtrace[index],
          "mismatched traces: #{backtrace[index].inspect} did not match #{pattern.inspect}"
    end
  end

  def test_backtrace_location_label # GH-5162
    [__method__].map do
      location = Thread.current.backtrace_locations[1]
      assert_equal 'test_backtrace_location_label', location.base_label
      assert_equal 'block in test_backtrace_location_label', location.label
    end
  end


  def test_backtrace_location_label_equal # GH-5163
    location = Thread.current.backtrace_locations[1]
    assert_equal __method__.to_s, location.base_label
    assert_equal __method__.to_s, location.label
    assert location.base_label.equal?(location.label)

    assert_end_with "test/jruby/test_thread_backtrace.rb:38:in 'test_backtrace_location_label_equal'", location.to_s
  end

  def assert_end_with(exp, str)
    assert_operator str, :end_with?, exp
  end

end
