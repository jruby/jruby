require 'jruby'
require 'benchmark'

StandardASMCompiler = org.jruby.compiler.StandardASMCompiler
NodeCompilerFactory = org.jruby.compiler.NodeCompilerFactory

fib_code = <<EOS
def fib(n)
  if n < 2
    n
  else
    fib(n - 2) + fib(n - 1)
  end
end
EOS

fib_node = JRuby.parse(fib_code, "EVAL")

def compile_to_class(node)
  context = StandardASMCompiler.new(node)
  NodeCompilerFactory.getCompiler(node).compile(node, context)

  context.loadClass
end

def compile_and_run(node)
  cls = compile_to_class(node)

  cls.new_instance.run(JRuby.runtime.current_context, JRuby.runtime.top_self)
end

# causes fib method to be defined
compile_and_run(fib_node)

puts Benchmark.measure { fib(30) }
puts Benchmark.measure { fib(30) }
puts Benchmark.measure { fib(30) }
puts Benchmark.measure { fib(30) }
puts Benchmark.measure { fib(30) }
