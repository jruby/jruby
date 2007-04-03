require 'test/unit'

class TestTraceFunc < Test::Unit::TestCase
  def test_class
    output = []
    set_trace_func proc { |event, file, line, id, binding, classname|
      output << sprintf("%s %s:%d %s %s", event, file, line, id ? id : 'nil', classname)
    }

    class << self
    end

    set_trace_func nil

    expected = ["line #{__FILE__}:10 test_class TestTraceFunc",
    "class #{__FILE__}:10 test_class TestTraceFunc",
    "end #{__FILE__}:10 test_class TestTraceFunc",
    "line #{__FILE__}:13 test_class TestTraceFunc",
    "c-call #{__FILE__}:13 set_trace_func Kernel"]
    assert_equal(expected, output);
  end

  def test_block_and_vars
    output = []
    set_trace_func proc { |event, file, line, id, binding, classname|
      output << sprintf("%s %s:%d %s %s", event, file, line, id, classname)
    }

    1.times {
      a = 1
      b = 2
    }

    set_trace_func nil

    expected = ["line #{__FILE__}:29 test_block_and_vars TestTraceFunc",
    "c-call #{__FILE__}:29 times Integer",
    "line #{__FILE__}:30 test_block_and_vars TestTraceFunc",
    "line #{__FILE__}:31 test_block_and_vars TestTraceFunc",
    "c-return #{__FILE__}:29 times Integer",
    "line #{__FILE__}:34 test_block_and_vars TestTraceFunc",
    "c-call #{__FILE__}:34 set_trace_func Kernel"]
    assert_equal(expected, output)
  end
end
