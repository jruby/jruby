require 'jruby'
require 'java'
require 'test/minirunit'

StandardASMCompiler = org.jruby.compiler.impl.StandardASMCompiler
NodeCompilerFactory = org.jruby.compiler.NodeCompilerFactory
Block = org.jruby.runtime.Block
IRubyObject = org.jruby.runtime.builtin.IRubyObject

def compile_to_class(src)
  node = JRuby.parse(src, "EVAL#{src.object_id}", false)
  context = StandardASMCompiler.new(node)
  NodeCompilerFactory.getCompiler(node).compile(node, context)

  context.loadClass(JRuby.runtime.getJRubyClassLoader)
end

def compile_and_run(src)
  cls = compile_to_class(src)

  cls.new_instance.run(JRuby.runtime.current_context, JRuby.runtime.top_self, IRubyObject[0].new, Block::NULL_BLOCK)
end

asgnFixnumCode = "a = 5; a"
asgnFloatCode = "a = 5.5; a"
asgnStringCode = "a = 'hello'; a"
asgnDStringCode = 'a = "hello#{42}"; a'
asgnEvStringCode = 'a = "hello#{1+42}"; a'
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

regexpLiteral = "/foo/"

match1 = "/foo/ =~ 'foo'"
match2 = "'foo' =~ /foo/"
match3 = ":aaa =~ /foo/"

iterBasic = "foo2('baz') { 4 }"

defBasic = "def foo3(arg); arg + '2'; end"

test_no_exception {
  compile_to_class(asgnFixnumCode);
}

test_equal(5, compile_and_run(asgnFixnumCode))
test_equal(5.5, compile_and_run(asgnFloatCode))
test_equal('hello', compile_and_run(asgnStringCode))
test_equal('hello42', compile_and_run(asgnDStringCode))
test_equal('hello43', compile_and_run(asgnEvStringCode))
test_equal(/foo/, compile_and_run(regexpLiteral))
test_equal(nil, compile_and_run('$2'))
test_equal(0, compile_and_run(match1))
test_equal(0, compile_and_run(match2))
test_equal(false, compile_and_run(match3))

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
test_equal(2, compile_and_run("2 if true"))
test_equal(3, compile_and_run(unlessCode))
test_equal(3, compile_and_run("3 unless false"))
test_equal(6, compile_and_run(whileCode))
test_equal('baz', compile_and_run(iterBasic))
compile_and_run(defBasic)
test_equal('hello2', foo3('hello'))

test_equal(2, compile_and_run(andCode))
test_equal(nil, compile_and_run(andShortCode));
test_equal(4, compile_and_run(beginCode));

class << Object
  alias :old_method_added :method_added
  def method_added(sym)
    $method_added = sym
    old_method_added(sym)
  end
end
test_no_exception {
  compile_and_run("alias :to_string :to_s")
  to_string
  test_equal(:to_string, $method_added)
}

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

yieldInBlock = <<EOS
def foo
  bar { yield }
end
def bar
  yield
end
foo { 1 }
EOS

test_equal(1, compile_and_run(yieldInBlock))

yieldInProc = <<EOS
def foo
  proc { yield }
end
p = foo { 1 }
p.call
EOS

test_equal(1, compile_and_run(yieldInProc))

test_equal({}, compile_and_run("{}"))
test_equal({:foo => :bar}, compile_and_run("{:foo => :bar}"))

test_equal(1..2, compile_and_run("1..2"))

# FIXME: These tests aren't quite right..only the first one should allow self.a to be accessed
# The other two should fail because the a accessor is private at top level. Only attr assigns
# should be made into "variable" call types and allowed through. I need a better way to
# test these, and the too-permissive visibility should be fixed.
test_equal(1, compile_and_run("def a=(x); 2; end; self.a = 1"))

test_equal(1, compile_and_run("def a; 1; end; def a=(arg); fail; end; self.a ||= 2"))
#test_equal([1, 1], compile_and_run("def a; @a; end; def a=(arg); @a = arg; 4; end; x = self.a ||= 1; [x, self.a]"))
test_equal(nil, compile_and_run("def a; nil; end; def a=(arg); fail; end; self.a &&= 2"))
#test_equal([1, 1], compile_and_run("def a; @a; end; def a=(arg); @a = arg; end; @a = 3; x = self.a &&= 1; [x, self.a]"))

test_equal(1, compile_and_run("def foo; $_ = 1; bar; $_; end; def bar; $_ = 2; end; foo"))

# test empty bodies
test_no_exception {
  test_equal(nil, compile_and_run(whileNoBody))
}

test_no_exception {
  # fcall with empty block
  test_equal(nil, compile_and_run("proc { }.call"))
  # call with empty block
  # FIXME: can't call proc this way, it's private
  #test_equal(nil, compile_and_run("self.proc {}.call"))
}

# blocks with some basic single arguments
test_no_exception {
  test_equal(1, compile_and_run("a = 0; [1].each {|a|}; a"))
  test_equal(1, compile_and_run("a = 0; [1].each {|x| a = x}; a"))
  test_equal(1, compile_and_run("[1].each {|@a|}; @a"))
  # make sure incoming array isn't treated as args array
  test_equal([1], compile_and_run("[[1]].each {|@a|}; @a"))
}

# blocks with unsupported arg layouts should still raise error
test_exception {
  compile_and_run("1.times {|@@a|}")
}
test_exception {
  compile_and_run("1.times {|a[0]|}")
}
test_exception {
  compile_and_run("1.times {|x, y|}")
}
test_exception {
  compile_and_run("1.times {|*x|}")
}

=begin
class_string = <<EOS
class CompiledClass1
  def foo
    "cc1"
  end
end
CompiledClass1.new.foo
EOS

test_equal("cc1", compile_and_run(class_string))

module_string = <<EOS
module CompiledModule1
  def bar
    "cm1"
  end
end

class CompiledClass2
  include CompiledModule1
end

CompiledClass2.new.bar
EOS

test_equal("cm1", compile_and_run(module_string))
=end

# opasgn with anything other than || or && was broken
class Holder
  attr_accessor :value
end
$h = Holder.new
test_equal(1, compile_and_run("$h.value ||= 1"))
test_equal(2, compile_and_run("$h.value &&= 2"))
test_equal(3, compile_and_run("$h.value += 1"))

# opt args
optargs_method = <<EOS
def foo(a, b = 1)
  [a, b]
end
EOS
test_no_exception {
  compile_and_run(optargs_method)
}
test_equal([1, 1], compile_and_run("foo(1)"))
test_equal([1, 2], compile_and_run("foo(1, 2)"))
test_exception { compile_and_run("foo(1, 2, 3)") }

# we do not compile opt args that cause other vars to be assigned, as in def (a=(b=1))
test_exception { compile_and_run("def foo(a=(b=1)); end")}
test_exception { compile_and_run("def foo(a, b=(c=1)); end")}