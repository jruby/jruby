require 'jruby'
require 'test/minirunit'

StandardASMCompiler = org.jruby.compiler.StandardASMCompiler
NodeCompilerFactory = org.jruby.compiler.NodeCompilerFactory

asgnFixnumCode = JRuby.parse("a = 5; a", "EVAL")
asgnStringCode = JRuby.parse("a = 'hello'; a", "EVAL")
arrayCode = JRuby.parse("['hello', 5, ['foo', 6]]", "EVAL")
fcallCode = JRuby.parse("foo('bar')", "EVAL")
callCode = JRuby.parse("'bar'.capitalize", "EVAL")
ifCode = JRuby.parse("if 1 == 1; 2; else; 3; end", "EVAL")
unlessCode = JRuby.parse("unless 1 == 1; 2; else; 3; end", "EVAL")
whileCode = JRuby.parse("a = 0; while a < 5; a = a + 2; end; a", "EVAL")

iterBasic = JRuby.parse("foo2('baz') { 4 }", "EVAL");

defBasic = JRuby.parse("def foo3(arg); arg + '2'; end", "EVAL")

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
  compile_to_class(asgnFixnumCode);
}

test_equal(5, compile_and_run(asgnFixnumCode))
test_equal('hello', compile_and_run(asgnStringCode))

def foo(arg)
  arg + '2'
end

def foo2(arg)
  arg
end

test_equal(['hello', 5, ['foo', 6]], compile_and_run(arrayCode))
test_equal('bar2', compile_and_run(fcallCode))
test_equal('Bar', compile_and_run(callCode))
test_equal(2, compile_and_run(ifCode))
test_equal(3, compile_and_run(unlessCode))
test_equal(6, compile_and_run(whileCode))
test_equal('baz', compile_and_run(iterBasic))

compile_and_run(defBasic)
test_equal('hello2', foo3('hello'))
