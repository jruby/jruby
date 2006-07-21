require 'java'
require 'jruby'

include_class "org.jruby.ast.executable.InstructionCompiler2"
include_class "java.io.FileOutputStream"
include_class "org.jruby.util.JRubyClassLoader"
include_class "org.jruby.runtime.builtin.IRubyObject"

compiler = InstructionCompiler2.new("MyCompiledScript", __FILE__);

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

fib_iter_java_str = <<EOS
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
EOS

# other tests
comp_test_str = <<EOS
def comp_test
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
end
EOS

# parse and compile
fib_java_n = JRuby.parse(fib_java_str, __FILE__);
fib_iter_java_n = JRuby.parse(fib_iter_java_str, __FILE__);
comp_test_n = JRuby.parse(comp_test_str, __FILE__);
begin
  fib_java_n.accept(compiler);
  fib_iter_java_n.accept(compiler);
  comp_test_n.accept(compiler);
rescue Exception => e
  puts e
  exit(1)
end

# create the class
script_class = JRubyClassLoader.new.define_class("MyCompiledScript", compiler.class_writer.to_byte_array())

# write it out, just for fun
FileOutputStream.new("MyCompiledScript.class").write(compiler.class_writer.to_byte_array());

# bind method to Kernel#fib_java
cbf = JRuby.runtime.callback_factory(script_class)
fib_callback = cbf.get_singleton_method("fib_java", IRubyObject.java_class)
compiler.define_module_function(JRuby.runtime, "Kernel", "fib_java", fib_callback);
fib_iter_callback = cbf.get_singleton_method("fib_iter_java", IRubyObject.java_class)
compiler.define_module_function(JRuby.runtime, "Kernel", "fib_iter_java", fib_iter_callback);
comp_callback = cbf.get_singleton_method("comp_test", IRubyObject.java_class)
compiler.define_module_function(JRuby.runtime, "Kernel", "comp_test", comp_callback);

# a simple benchmarking function
def time(str)
  t = Time.now
  yield
  puts "Time for #{str}: #{Time.now - t}"
end

# time interpreted versus compiled
time("bi-recursive, interpreted") { fib_ruby(30) }
time("bi-recursive, compiled") { fib_java(30) }
time("bi-recursive, interpreted") { fib_ruby(30) }
time("bi-recursive, compiled") { fib_java(30) }

time("iterative, interpreted") { fib_iter_ruby(500000) }
time("iterative, compiled") { fib_iter_java(500000) }
time("iterative, interpreted") { fib_iter_ruby(500000) }
time("iterative, compiled") { fib_iter_java(500000) }

p comp_test(nil)
