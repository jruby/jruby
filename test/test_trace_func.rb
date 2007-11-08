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

  def bogus_method
  end

  def sample_method(a, b)
    a + b
  end

  def test_method_trace
    output = []
    
    set_trace_func proc { |event, file, line, id, binding, classname|
      output << sprintf("%10s %s:%-2d %18s %14s", event, file, line, id, classname)
    }
    
    sample_method(1, 1)
    
    set_trace_func nil
    
    expected = ["      line ./test/test_trace_func.rb:60  test_method_trace  TestTraceFunc",
                "      call ./test/test_trace_func.rb:49      sample_method  TestTraceFunc",
                "      line ./test/test_trace_func.rb:50      sample_method  TestTraceFunc",
                "    c-call ./test/test_trace_func.rb:50                  +         Fixnum",
                "  c-return ./test/test_trace_func.rb:50                  +         Fixnum",
                "    return ./test/test_trace_func.rb:51      sample_method  TestTraceFunc",
                "      line ./test/test_trace_func.rb:62  test_method_trace  TestTraceFunc",
                "    c-call ./test/test_trace_func.rb:62     set_trace_func         Kernel"];
    assert_equal(expected, output)
  end

  def test_trace_binding
    a = true
    expected = [["a", "expected", "results"],
                ["a", "expected", "results"],
                ["a", "expected", "results"],
                ["a", "expected", "results"],
                [],
                [],
                ["a", "expected", "results"],
                ["a", "expected", "results"]]
    results = []
    set_trace_func proc { |event, file, line, id, binding, classname| results << eval('local_variables', binding) }

    1.to_i # c-call, two traces
    # newline, one trace
    bogus_method # call, two traces
    # newline, one trace
    set_trace_func nil # c-call, two traces
 
    assert_equal(expected, results)
  end
end

