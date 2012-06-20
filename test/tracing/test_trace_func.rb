require 'test/unit'

class TestTraceFunc < Test::Unit::TestCase

  def setup
    super
    # put back normal method_added so ours doesn't pollute traces
    class << Object; alias method_added java_package_method_added; end
    @test_dir = File.dirname(__FILE__)
  end

  def test_class
    output = []
    set_trace_func proc { |event, file, line, id, binding, classname|
      output << sprintf("%s %s:%d %s %s", event, file, line, id ? id : 'nil', classname)
    }

    class << self
    end

    set_trace_func nil

    line = __LINE__ - 5
    expected = ["line #{__FILE__}:#{line} test_class TestTraceFunc",
      "class #{__FILE__}:#{line} test_class TestTraceFunc",
      "end #{__FILE__}:#{line} test_class TestTraceFunc",
      "line #{__FILE__}:#{line + 3} test_class TestTraceFunc",
      "c-call #{__FILE__}:#{line + 3} set_trace_func Kernel"]
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

    line = __LINE__ - 7
    expected = ["line #{__FILE__}:#{line} test_block_and_vars TestTraceFunc",
      "c-call #{__FILE__}:#{line} times Fixnum",
      "line #{__FILE__}:#{line + 1} test_block_and_vars TestTraceFunc",
      "line #{__FILE__}:#{line + 2} test_block_and_vars TestTraceFunc",
      "c-return #{__FILE__}:#{line} times Fixnum",
      "line #{__FILE__}:#{line + 5} test_block_and_vars TestTraceFunc",
      "c-call #{__FILE__}:#{line + 5} set_trace_func Kernel"]
    assert_equal(expected, output)
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

    line = __LINE__ - 4
    expected = ["      line #{__FILE__}:#{line}  test_method_trace  TestTraceFunc",
                "      call #{__FILE__}:#{line - 11}      sample_method  TestTraceFunc",
                "      line #{__FILE__}:#{line - 10}      sample_method  TestTraceFunc",
                "    c-call #{__FILE__}:#{line - 10}                  +         Fixnum",
                "  c-return #{__FILE__}:#{line - 10}                  +         Fixnum",
                "    return #{__FILE__}:#{line - 10}      sample_method  TestTraceFunc",
                "      line #{__FILE__}:#{line + 2}  test_method_trace  TestTraceFunc",
                "    c-call #{__FILE__}:#{line + 2}     set_trace_func         Kernel"];
    assert_equal(expected, output)
  end

  def bogus_method
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
    set_trace_func proc { |event, file, line, id, binding, classname|
      results << eval('local_variables', binding)
    }

    1.to_i # c-call, two traces
    # newline, one trace
    bogus_method # call, two traces
    # newline, one trace
    set_trace_func nil # c-call, two traces

    assert_equal(expected, results)
  end

  def test_load_trace
    output = []

    set_trace_func proc { |event, file, line, id, binding, classname|
      output << sprintf("%s %d %s %s", event, line, id ? id : 'nil', classname)
    }

    require("#@test_dir/dummy.rb")

    set_trace_func nil

    line = __LINE__ - 4
    expected = ["line #{line} test_load_trace TestTraceFunc",
      "c-call #{line} require Kernel",
      "line 1 nil false",
      "c-call 1 inherited Class",
      "c-return 1 inherited Class",
      "class 1 nil false",
      "end 1 nil false",
      "c-return #{line} require Kernel",
      "line #{line + 2} test_load_trace TestTraceFunc",
      "c-call #{line + 2} set_trace_func Kernel"]

    assert_equal(expected, output);
  end

  def test_require_trace_full_paths # JRUBY-2722
    output = []

    set_trace_func proc { |event, file, line, id, binding, classname|
      unless event =~ /c-.*/
        output << sprintf("%s %s %d %s %s", file, event, line, id ? id : 'nil', classname)
      end
    }

    require("./test/tracing/dummy1.rb")

    set_trace_func nil

    line = __LINE__ - 4
    expected = ["#{__FILE__} line #{line} test_require_trace_full_paths TestTraceFunc",
      "./test/tracing/dummy1.rb line 1 nil false",
      "./test/tracing/dummy1.rb class 1 nil false",
      "./test/tracing/dummy1.rb end 1 nil false",
      "#{__FILE__} line #{line + 2} test_require_trace_full_paths TestTraceFunc"]

    assert_equal(expected, output);
  end

  def test_require_trace # JRUBY-2722
    output = []
    set_trace_func proc { |event, file, line, id, binding, classname|
      unless event =~ /c-.*/
        output << sprintf("%s %s %d %s %s", File.basename(file), event, line, id ? id : 'nil', classname)
      end
    }

    require("#@test_dir/dummy2.rb")

    set_trace_func nil

    line = __LINE__ - 4
    expected = ["test_trace_func.rb line #{line} test_require_trace TestTraceFunc",
      "dummy2.rb line 1 nil false",
      "dummy2.rb class 1 nil false",
      "dummy2.rb end 1 nil false",
      "test_trace_func.rb line #{line + 2} test_require_trace TestTraceFunc"]

    assert_equal(expected, output);
  end

  def test_return # JRUBY-2723
    output = []
    set_trace_func proc { |event, file, line, id, binding, classname|
      output << sprintf("%s %s %d %s %s", File.basename(file), event, line, id ? id : 'nil', classname)
    }

    require "#@test_dir/other.rb"
    in_other

    set_trace_func nil

    line = __LINE__ - 5
    expected = ["test_trace_func.rb line #{line} test_return TestTraceFunc",
      "test_trace_func.rb c-call #{line} require Kernel",
      "other.rb line 1 nil false",
      "other.rb c-call 1 method_added Module",
      "other.rb c-return 1 method_added Module",
      "test_trace_func.rb c-return #{line} require Kernel",
      "test_trace_func.rb line #{line + 1} test_return TestTraceFunc",
      "other.rb call 1 in_other Object",
      "other.rb line 2 in_other Object",
      "other.rb c-call 2 sleep Kernel",
      "other.rb c-return 2 sleep Kernel",
      "other.rb return 2 in_other Object",
      "test_trace_func.rb line #{line + 3} test_return TestTraceFunc",
      "test_trace_func.rb c-call #{line + 3} set_trace_func Kernel"]

    assert_equal(expected, output);
  end

  # JRUBY-2815
  def test_trace_over_system_call
    output = []
    set_trace_func(proc do |event, file, line, id, binding, classname|
      unless file =~ /path_helper\.rb/
        output << sprintf("%s %s %d %s %s", File.basename(file), event, line, id ? id : 'nil', classname)
      end
    end)

    system('echo .')
    set_trace_func nil

    line = __LINE__ - 3
    expected = ["test_trace_func.rb line #{line} test_trace_over_system_call TestTraceFunc",
       "test_trace_func.rb c-call #{line} system Kernel",
       "test_trace_func.rb c-return #{line} system Kernel",
       "test_trace_func.rb line #{line + 1} test_trace_over_system_call TestTraceFunc",
       "test_trace_func.rb c-call #{line + 1} set_trace_func Kernel"]
    assert_equal expected, output
  end

end

