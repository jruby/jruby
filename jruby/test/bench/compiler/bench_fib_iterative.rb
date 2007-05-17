require 'jruby'
require 'benchmark'

StandardASMCompiler = org.jruby.compiler.impl.StandardASMCompiler
NodeCompilerFactory = org.jruby.compiler.NodeCompilerFactory

fib_iter_code = <<EOS
def fib(n)
  i = 0
  j = 1
  cur = 1
  while cur <= n
    k = i
    i = j
    j = k + j
    cur = cur + 1
  end
end
EOS

fib_iter = JRuby.parse(fib_iter_code, "EVAL")

def compile_to_class(node)
  context = StandardASMCompiler.new(node)
  NodeCompilerFactory.getCompiler(node).compile(node, context)

  context.loadClass(JRuby.runtime)
end

def compile_and_run(node)
  cls = compile_to_class(node)

  cls.new_instance.run(JRuby.runtime.current_context, JRuby.runtime.top_self, nil, nil)
end

# causes fib method to be defined
compile_and_run(fib_iter)

puts Benchmark.measure { fib(300000) }
puts Benchmark.measure { fib(300000) }
puts Benchmark.measure { fib(300000) }
puts Benchmark.measure { fib(300000) }
puts Benchmark.measure { fib(300000) }

