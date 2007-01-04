require 'jruby'
require 'benchmark'

StandardASMCompiler = org.jruby.compiler.StandardASMCompiler
NodeCompilerFactory = org.jruby.compiler.NodeCompilerFactory

control_code = <<EOS
def control
  a = 5; 
  i = 0;
  while i < 100000
    a; a; a; a; a; a; a; a; a; a;
    a; a; a; a; a; a; a; a; a; a;
    a; a; a; a; a; a; a; a; a; a;
    a; a; a; a; a; a; a; a; a; a;
    a; a; a; a; a; a; a; a; a; a;
    a; a; a; a; a; a; a; a; a; a;
    a; a; a; a; a; a; a; a; a; a;
    a; a; a; a; a; a; a; a; a; a;
    a; a; a; a; a; a; a; a; a; a;
    a; a; a; a; a; a; a; a; a; a;
    i = i + 1;
  end
end
EOS

test_code = <<EOS
def test
  a = 5; 
  i = 0;
  while i < 100000
    a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i;
    a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i;
    a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i;
    a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i;
    a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i;
    a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i;
    a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i;
    a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i;
    a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i;
    a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i; a.to_i;
    i = i + 1;
  end
end
EOS

control_node = JRuby.parse(control_code, "EVAL")
test_node = JRuby.parse(test_code, "EVAL")

def compile_to_class(node)
  context = StandardASMCompiler.new(node)
  NodeCompilerFactory.getCompiler(node).compile(node, context)

  context.loadClass
end

def compile_and_run(node)
  cls = compile_to_class(node)

  cls.new_instance.run(JRuby.runtime.current_context, JRuby.runtime.top_self)
end

compile_and_run(control_node)
compile_and_run(test_node)

puts "Control"
puts Benchmark.measure { control }
puts Benchmark.measure { control }
puts Benchmark.measure { control }
puts Benchmark.measure { control }
puts Benchmark.measure { control }

puts "Test"
puts Benchmark.measure { test }
puts Benchmark.measure { test }
puts Benchmark.measure { test }
puts Benchmark.measure { test }
puts Benchmark.measure { test }
