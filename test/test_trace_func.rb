require 'test/unit'

class TestTraceFunc < Test::Unit::TestCase
  def test_class
    output = []
    set_trace_func proc { |event, file, line, id, binding, classname|
      output << sprintf("%8s %20s:%-2d %10s %-8s\n", event, file, line, id ? id : 'nil', classname)
    }

    class << self
    end

    set_trace_func nil

    expected = ["    line test/test_trace_func.rb.rb:10 test_class TestTraceFunc\n",
      "   class test/test_trace_func.rb.rb:10 test_class TestTraceFunc\n",
      "     end test/test_trace_func.rb.rb:10 test_class TestTraceFunc\n",
      "    line test/test_trace_func.rb.rb:13 test_class TestTraceFunc\n",
      "  c-call test/test_trace_func.rb.rb:13 set_trace_func Kernel  \n"]
    assert_equal(expected, output);
  end

  def test_block_and_vars
    output = []
    set_trace_func proc { |event, file, line, id, binding, classname|
      output << sprintf("%8s %s:%-2d %10s %8s\n", event, file, line, id, classname)
    }

    1.times {
      a = 1
      b = 2
    }

    set_trace_func nil

    expected = ["    line test/test_trace_func.rb.rb:29 test_block_and_vars TestTraceFunc\n",
      "  c-call test/test_trace_func.rb.rb:29      times  Integer\n",
      "    line test/test_trace_func.rb.rb:30 test_block_and_vars TestTraceFunc\n",
      "    line test/test_trace_func.rb.rb:31 test_block_and_vars TestTraceFunc\n",
      "c-return test/test_trace_func.rb.rb:29      times  Integer\n",
      "    line test/test_trace_func.rb.rb:34 test_block_and_vars TestTraceFunc\n",
      "  c-call test/test_trace_func.rb.rb:34 set_trace_func   Kernel\n"]
    assert_equal(expected, output)
  end
end
