require 'java'
require 'jruby'

include_class "org.jruby.compiler.InstructionCompiler2"
include_class "java.io.FileOutputStream"
include_class "org.jruby.util.JRubyClassLoader"
include_class "org.jruby.runtime.builtin.IRubyObject"

compiler = InstructionCompiler2.new();

# pure ruby version
def fib_ruby(n)
  if n < 2
    n
  else
    fib_ruby(n - 2) + fib_ruby(n - 1)
  end
end

# version to be parsed and compiled
fib_java_str = <<EOS
def fib_java(n)
	if n < 2
	  n
	else
	  fib_java(n - 2) + fib_java(n - 1)
	end
end

def fib_iter_java(n)
   i = 0
   j = 1
   cur = 1
   while cur <= n
     k = i
     i = j
     j = k + j
     cur = cur + 1
   end
   i
end

def comp_test
  begin
  nil
  1111111111111111111111111111111111111111111111111111111111
  1.0
  false
  true
  [1, 2, 3, 4, 5]
  "hello"
  x = 1
  while (x < 5)
    puts x
    x = x + 1
  end
    @@x = 5
    p @@x
end
end
EOS

def fib_iter_ruby(n)
   i = 0
   j = 1
   cur = 1
   while cur <= n
     k = i
     i = j
     j = k + j
     cur = cur + 1
   end
   i
end

# parse and compile
fib_java_n = JRuby.parse(fib_java_str, __FILE__);
begin
  class_and_method = compiler.compile("MyCompiledScript", __FILE__, fib_java_n);
rescue Exception => e
  puts e
  exit(1)
end

# create the classloader
script_class_loader = JRubyClassLoader.new

# write it out, just for fun
classes = {}
compiler.class_writers.each do |k,v|
  FileOutputStream.new("#{k}.class").write(v.to_byte_array())
  classes[k] = script_class_loader.define_class(k, v.to_byte_array())
end

# bind method to Kernel#fib_java
script_class = classes["MyCompiledScript$MultiStub0"]
stub = script_class.newInstance
compiler.define_module_function(JRuby.runtime, "Kernel", "fib_java", stub, 1, org.jruby.runtime.Arity.singleArgument, org.jruby.runtime.Visibility::PUBLIC);
compiler.define_module_function(JRuby.runtime, "Kernel", "fib_iter_java", stub, 2, org.jruby.runtime.Arity.singleArgument, org.jruby.runtime.Visibility::PUBLIC);
compiler.define_module_function(JRuby.runtime, "Kernel", "comp_test", stub, 3, org.jruby.runtime.Arity.noArguments, org.jruby.runtime.Visibility::PUBLIC);

# a simple benchmarking function
def time(str)
  t = Time.now
  yield
  puts "Time for #{str}: #{Time.now - t}"
end

p comp_test(nil)

# time interpreted versus compiled
time("bi-recursive, interpreted") { fib_ruby(30) }
time("bi-recursive, compiled") { fib_java(30) }
time("bi-recursive, interpreted") { fib_ruby(30) }
time("bi-recursive, compiled") { fib_java(30) }

time("iterative, interpreted") { fib_iter_ruby(500000) }
time("iterative, compiled") { fib_iter_java(500000) }
time("iterative, interpreted") { fib_iter_ruby(500000) }
time("iterative, compiled") { fib_iter_java(500000) }

