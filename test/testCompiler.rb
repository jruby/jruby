require 'jruby'
require 'test/minirunit'

StandardASMCompiler = org.jruby.compiler.impl.StandardASMCompiler
NodeCompilerFactory = org.jruby.compiler.NodeCompilerFactory

asgnFixnumCode = JRuby.parse("a = 5; a", "EVAL1")
asgnStringCode = JRuby.parse("a = 'hello'; a", "EVAL2")
arrayCode = JRuby.parse("['hello', 5, ['foo', 6]]", "EVAL3")
fcallCode = JRuby.parse("foo('bar')", "EVAL4")
callCode = JRuby.parse("'bar'.capitalize", "EVAL5")
ifCode = JRuby.parse("if 1 == 1; 2; else; 3; end", "EVAL6")
unlessCode = JRuby.parse("unless 1 == 1; 2; else; 3; end", "EVAL7")
whileCode = JRuby.parse("a = 0; while a < 5; a = a + 2; end; a", "EVAL8")
whileNoBody = JRuby.parse("$foo = false; def flip; $foo = !$foo; $foo; end; while flip; end", "EVAL8_1")
andCode = JRuby.parse("1 && 2", "EVAL9");
andShortCode = JRuby.parse("nil && 3", "EVAL10");
beginCode = JRuby.parse("begin; a = 4; end; a", "beginCode");

iterBasic = JRuby.parse("foo2('baz') { 4 }", "EVAL11");

defBasic = JRuby.parse("def foo3(arg); arg + '2'; end", "EVAL12")

def compile_to_class(node)
  context = StandardASMCompiler.new(node)
  NodeCompilerFactory.getCompiler(node).compile(node, context)

  context.loadClass(JRuby.runtime)
end

def compile_and_run(node)
  cls = compile_to_class(node)

  cls.new_instance.run(JRuby.runtime.current_context, JRuby.runtime.top_self, nil, nil)
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
#test_no_exception {
    test_equal(nil, compile_and_run(whileNoBody))
#}
#test_equal('baz', compile_and_run(iterBasic))
compile_and_run(defBasic)
test_equal('hello2', foo3('hello'))

test_equal(2, compile_and_run(andCode))
test_equal(nil, compile_and_run(andShortCode));
test_equal(4, compile_and_run(beginCode));