require 'jruby'
require 'test/minirunit'

StandardASMCompiler = org.jruby.compiler.StandardASMCompiler
NodeCompilerFactory = org.jruby.compiler.NodeCompilerFactory

asgnFixnumNode = JRuby.parse("a = 5; a", "EVAL")
asgnStringNode = JRuby.parse("a = 'hello'; a", "EVAL")
fcallNode = JRuby.parse("foo('bar')", "EVAL")
callNode = JRuby.parse("'bar'.capitalize", "EVAL")
ifNode = JRuby.parse("if 1 == 1; 2; else; 3; end", "EVAL")

def compile_to_class(node)
  context = StandardASMCompiler.new(node)
  NodeCompilerFactory.getCompiler(node).compile(node, context)

  context.loadClass
end

def compile_and_run(node)
  cls = compile_to_class(node)

  cls.new_instance.run(JRuby.runtime.current_context, JRuby.runtime.top_self)
end

test_no_exception {
  compile_to_class(asgnFixnumNode);
}

test_equal(5, compile_and_run(asgnFixnumNode))
test_equal('hello', compile_and_run(asgnStringNode))

def foo(arg)
  arg + '2'
end

test_equal('bar2', compile_and_run(fcallNode))
test_equal('Bar', compile_and_run(callNode))
test_equal(2, compile_and_run(ifNode));