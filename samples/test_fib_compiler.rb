require 'java'
require 'jruby'

include_class "org.jruby.ast.executable.InstructionCompiler2"
include_class "java.io.FileOutputStream"
include_class "org.jruby.util.JRubyClassLoader"
include_class "org.jruby.runtime.builtin.IRubyObject"

compiler = InstructionCompiler2.new("MyCompiledScript", __FILE__);

# pure ruby version
def fib_ruby(n)
  nil
  1111111111111111111111111111111111111111111111111111111111
  1.0
  false
  true
  [1, 2, 3, 4, 5]
  "hello"
  if n < 2
    n
  else
    fib_ruby(n - 2) + fib_ruby(n - 1)
  end
end

# version to be parsed and compiled
fib_java_str = <<EOS
def fib_java(n)
  nil
  1111111111111111111111111111111111111111111111111111111111
  1.0
  false
  true
  [1, 2, 3, 4, 5]
  "hello"
	if n < 2
	  n
	else
	  fib_java(n - 2) + fib_java(n - 1)
	end
end
EOS

# parse and compile
n = JRuby.parse(fib_java_str, __FILE__);
begin
  n.accept(compiler);
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

# a simple benchmarking function
def time(str)
  t = Time.now
  yield
  puts "Time for #{str}: #{Time.now - t}"
end

# time interpreted versus compiled
time("interpreted") { fib_ruby(20) }
time("compiled") { fib_java(20) }
