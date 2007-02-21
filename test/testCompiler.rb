require 'jruby'
require 'test/minirunit'

StandardASMCompiler = org.jruby.compiler.impl.StandardASMCompiler
NodeCompilerFactory = org.jruby.compiler.NodeCompilerFactory

def compile_to_class(src)
  node = JRuby.parse(src, "EVAL#{src.object_id}")
  context = StandardASMCompiler.new(node)
  NodeCompilerFactory.getCompiler(node).compile(node, context)

  context.loadClass(JRuby.runtime)
end

def compile_and_run(src)
  cls = compile_to_class(src)

  cls.new_instance.run(JRuby.runtime.current_context, JRuby.runtime.top_self, nil, nil)
end

asgnFixnumCode = "a = 5; a"
asgnStringCode = "a = 'hello'; a"
arrayCode = "['hello', 5, ['foo', 6]]"
fcallCode = "foo('bar')"
callCode = "'bar'.capitalize"
ifCode = "if 1 == 1; 2; else; 3; end"
unlessCode = "unless 1 == 1; 2; else; 3; end"
whileCode = "a = 0; while a < 5; a = a + 2; end; a"
whileNoBody = "$foo = false; def flip; $foo = !$foo; $foo; end; while flip; end"
andCode = "1 && 2"
andShortCode = "nil && 3"
beginCode = "begin; a = 4; end; a"

iterBasic = "foo2('baz') { 4 }"

defBasic = "def foo3(arg); arg + '2'; end"

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

# Some complicated block var stuff
blocksCode = <<-EOS
def a
  yield 3
end

arr = []
x = 1
1.times { 
  y = 2
  arr << x
  x = 3
  a { 
    arr << y
    y = 4
    arr << x
    x = 5
  }
  arr << y
  arr << x
  x = 6
}
arr << x
EOS

test_equal([1,2,3,4,5,6], compile_and_run(blocksCode))
