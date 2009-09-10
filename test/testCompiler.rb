require 'jruby'
require 'java'
require 'test/minirunit'

StandardASMCompiler = org.jruby.compiler.impl.StandardASMCompiler
ASTCompiler = org.jruby.compiler.ASTCompiler
ASTInspector = org.jruby.compiler.ASTInspector
Block = org.jruby.runtime.Block
IRubyObject = org.jruby.runtime.builtin.IRubyObject

def silence_warnings
  verb = $VERBOSE
  $VERBOSE = nil
  yield
ensure
  $VERBOSE = verb
end

def compile_to_class(src)
  node = JRuby.parse(src, "testCompiler#{src.object_id}", false)
  filename = node.position.file
  classname = filename.sub("/", ".").sub("\\", ".").sub(".rb", "")
  inspector = ASTInspector.new
  inspector.inspect(node)
  context = StandardASMCompiler.new(classname, filename)
  compiler = ASTCompiler.new
  compiler.compileRoot(node, context, inspector)

  context.loadClass(JRuby.runtime.getJRubyClassLoader)
end

def compile_and_run(src)
  cls = compile_to_class(src)

  cls.new_instance.load(JRuby.runtime.current_context, JRuby.runtime.top_self, IRubyObject[0].new, Block::NULL_BLOCK)
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

# clone this since we're generating classnames based on object_id above
test_equal(5, compile_and_run(asgnFixnumCode.clone))
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
# statement-based 'and' with modifier
test_equal(1, compile_and_run("1 and 2 if 3; 1"))
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
test_equal([1, 1], compile_and_run("def a; @a; end; def a=(arg); @a = arg; 4; end; x = self.a ||= 1; [x, self.a]"))
test_equal(nil, compile_and_run("def a; nil; end; def a=(arg); fail; end; self.a &&= 2"))
test_equal([1, 1], compile_and_run("def a; @a; end; def a=(arg); @a = arg; end; @a = 3; x = self.a &&= 1; [x, self.a]"))

test_equal(1, compile_and_run("def foo; $_ = 1; bar; $_; end; def bar; $_ = 2; end; foo"))

# test empty bodies
test_no_exception {
  test_equal(nil, compile_and_run(whileNoBody))
}

test_no_exception {
  # fcall with empty block
  test_equal(nil, compile_and_run("def myfcall; yield; end; myfcall {}"))
  # call with empty block
  test_equal(nil, compile_and_run("def mycall; yield; end; public :mycall; self.mycall {}"))
}

# blocks with some basic single arguments
test_no_exception {
  test_equal(1, compile_and_run("a = 0; [1].each {|a|}; a"))
  test_equal(1, compile_and_run("a = 0; [1].each {|x| a = x}; a"))
  test_equal(1, compile_and_run("[1].each {|@a|}; @a"))
  # make sure incoming array isn't treated as args array
  test_equal([1], compile_and_run("[[1]].each {|@a|}; @a"))
}

# blocks with tail (rest) arguments
test_no_exception {
  test_equal([2,3], compile_and_run("[[1,2,3]].each {|x,*y| break y}"))
  test_equal([], compile_and_run("1.times {|x,*y| break y}"))
  test_no_exception { compile_and_run("1.times {|x,*|}")}
}

compile_and_run("1.times {|@@a|}")
compile_and_run("a = []; 1.times {|a[0]|}")

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

# opt args that cause other vars to be assigned, as in def (a=(b=1))
compile_and_run("def foo(a=(b=1)); end")
compile_and_run("def foo(a, b=(c=1)); end")

class CoercibleToArray
  def to_ary
    [2, 3]
  end
end

# argscat
def foo(a, b, c)
  return a, b, c
end

test_equal([1, 2, 3], compile_and_run("foo(1, *[2, 3])"))
test_equal([1, 2, 3], compile_and_run("foo(1, *CoercibleToArray.new)"))

# multiple assignment
test_equal([1, 2, 3], compile_and_run("a = nil; 1.times { a, b, @c = 1, 2, 3; a = [a, b, @c] }; a"))
test_equal([1, nil, nil], compile_and_run("a, (b, c) = 1; [a, b, c]"))
test_equal([1, 2, nil], compile_and_run("a, (b, c) = 1, 2; [a, b, c]"))
test_equal([1, 2, 3], compile_and_run("a, (b, c) = 1, [2, 3]; [a, b, c]"))
test_equal([1, 2, 3], compile_and_run("a, (b, c) = 1, CoercibleToArray.new; [a, b, c]"))

# until loops
test_equal(3, compile_and_run("a = 1; until a == 3; a += 1; end; a"))
test_equal(3, compile_and_run("a = 3; until a == 3; end; a"))

# dynamic regexp
test_equal([/foobar/, /foobaz/], compile_and_run('a = "bar"; b = []; while true; b << %r[foo#{a}]; break if a == "baz"; a = "baz"; end; b'))

# return
test_no_exception {
    test_equal(1, compile_and_run("def foo; 1; end; foo"))
    test_equal(nil, compile_and_run("def foo; return; end; foo"))
    test_equal(1, compile_and_run("def foo; return 1; end; foo"))
}

# reopening a class
test_no_exception {
    test_equal(3, compile_and_run("class Fixnum; def foo; 3; end; end; 1.foo"))
}

# singleton defs
test_equal("foo", compile_and_run("a = 'bar'; def a.foo; 'foo'; end; a.foo"))
test_equal("foo", compile_and_run("class Fixnum; def self.foo; 'foo'; end; end; Fixnum.foo"))
test_equal("foo", compile_and_run("def String.foo; 'foo'; end; String.foo"))

# singleton classes
test_equal("bar", compile_and_run("a = 'bar'; class << a; def bar; 'bar'; end; end; a.bar"))
test_equal("bar", compile_and_run("class Fixnum; class << self; def bar; 'bar'; end; end; end; Fixnum.bar"))
test_equal(class << Fixnum; self; end, compile_and_run("class Fixnum; def self.metaclass; class << self; self; end; end; end; Fixnum.metaclass"))

# some loop flow control tests
test_equal(nil, compile_and_run("a = true; b = while a; a = false; break; end; b"))
test_equal(1, compile_and_run("a = true; b = while a; a = false; break 1; end; b"))
test_equal(2, compile_and_run("a = 0; while true; a += 1; next if a < 2; break; end; a"))
test_equal(2, compile_and_run("a = 0; while true; a += 1; next 1 if a < 2; break; end; a"))
test_equal(2, compile_and_run("a = 0; while true; a += 1; redo if a < 2; break; end; a"))
test_equal(nil, compile_and_run("a = false; b = until a; a = true; break; end; b"))
test_equal(1, compile_and_run("a = false; b = until a; a = true; break 1; end; b"))
test_equal(2, compile_and_run("a = 0; until false; a += 1; next if a < 2; break; end; a"))
test_equal(2, compile_and_run("a = 0; until false; a += 1; next 1 if a < 2; break; end; a"))
test_equal(2, compile_and_run("a = 0; until false; a += 1; redo if a < 2; break; end; a"))
# same with evals
test_equal(nil, compile_and_run("a = true; b = while a; a = false; eval 'break'; end; b"))
test_equal(1, compile_and_run("a = true; b = while a; a = false; eval 'break 1'; end; b"))
test_equal(2, compile_and_run("a = 0; while true; a += 1; eval 'next' if a < 2; eval 'break'; end; a"))
test_equal(2, compile_and_run("a = 0; while true; a += 1; eval 'next 1' if a < 2; eval 'break'; end; a"))
test_equal(2, compile_and_run("a = 0; while true; a += 1; eval 'redo' if a < 2; eval 'break'; end; a"))
test_equal(nil, compile_and_run("a = false; b = until a; a = true; eval 'break'; end; b"))
test_equal(1, compile_and_run("a = false; b = until a; a = true; eval 'break 1'; end; b"))
test_equal(2, compile_and_run("a = 0; until false; a += 1; eval 'next' if a < 2; eval 'break'; end; a"))
test_equal(2, compile_and_run("a = 0; until false; a += 1; eval 'next 1' if a < 2; eval 'break'; end; a"))
test_equal(2, compile_and_run("a = 0; until false; a += 1; eval 'redo' if a < 2; eval 'break'; end; a"))

# non-local flow control with while loops
test_equal(2, compile_and_run("a = 0; 1.times { a += 1; redo if a < 2 }; a"))
test_equal(3, compile_and_run("def foo(&b); while true; b.call; end; end; foo { break 3 }"))
# this one doesn't work normally, so I wouldn't expect it to work here yet
#test_equal(2, compile_and_run("a = 0; 1.times { a += 1; eval 'redo' if a < 2 }; a"))
test_equal(3, compile_and_run("def foo(&b); while true; b.call; end; end; foo { eval 'break 3' }"))

# block pass node compilation
test_equal([false, true], compile_and_run("def foo; block_given?; end; p = proc {}; [foo(&nil), foo(&p)]"))
test_equal([false, true], compile_and_run("public; def foo; block_given?; end; p = proc {}; [self.foo(&nil), self.foo(&p)]"))

# backref nodes
test_equal(["foo", "foo", "bazbar", "barfoo", "foo"], compile_and_run("'bazbarfoobarfoo' =~ /(foo)/; [$~[0], $&, $`, $', $+]"))
test_equal(["", "foo ", "foo bar ", "foo bar foo "], compile_and_run("a = []; 'foo bar foo bar'.scan(/\\w+/) {a << $`}; a"))

# argspush
test_equal("fasdfo", compile_and_run("a = 'foo'; y = ['o']; a[*y] = 'asdf'; a"))

# constnode, colon2node, and colon3node
const_code = <<EOS
A = 'a'; module X; B = 'b'; end; module Y; def self.go; [A, X::B, ::A]; end; end; Y.go
EOS
test_equal(["a", "b", "a"], compile_and_run(const_code))

# flip (taken from http://redhanded.hobix.com/inspect/hopscotchingArraysWithFlipFlops.html)
test_equal([1, 3, 5, 7, 9], compile_and_run("s = true; (1..10).reject { true if (s = !s) .. (s) }"))
test_equal([1, 4, 7, 10], compile_and_run("s = true; (1..10).reject { true if (s = !s) .. (s = !s) }"))
big_flip = <<EOS
s = true; (1..10).inject([]) do |ary, v|; ary << [] unless (s = !s) .. (s = !s); ary.last << v; ary; end
EOS
test_equal([[1, 2, 3], [4, 5, 6], [7, 8, 9], [10]], compile_and_run(big_flip))
big_triple_flip = <<EOS
s = true
(1..64).inject([]) do |ary, v|
    unless (s ^= v[2].zero?)...(s ^= !v[1].zero?)
        ary << []
    end
    ary.last << v
    ary
end
EOS
expected = [[1, 2, 3, 4, 5, 6, 7, 8],
      [9, 10, 11, 12, 13, 14, 15, 16],
      [17, 18, 19, 20, 21, 22, 23, 24],
      [25, 26, 27, 28, 29, 30, 31, 32],
      [33, 34, 35, 36, 37, 38, 39, 40],
      [41, 42, 43, 44, 45, 46, 47, 48],
      [49, 50, 51, 52, 53, 54, 55, 56],
      [57, 58, 59, 60, 61, 62, 63, 64]]
test_equal(expected, compile_and_run(big_triple_flip))

silence_warnings {
  # bug 1305, no values yielded to single-arg block assigns a null into the arg
  test_equal(NilClass, compile_and_run("def foo; yield; end; foo {|x| x.class}"))
}

# ensure that invalid classes and modules raise errors
AFixnum = 1;
test_exception(TypeError) { compile_and_run("class AFixnum; end")}
test_exception(TypeError) { compile_and_run("class B < AFixnum; end")}
test_exception(TypeError) { compile_and_run("module AFixnum; end")}

# attr assignment in multiple assign
test_equal("bar", compile_and_run("a = Object.new; class << a; attr_accessor :b; end; a.b, a.b = 'baz', 'bar'; a.b"))
test_equal(["foo", "bar"], compile_and_run("a = []; a[0], a[1] = 'foo', 'bar'; a"))

# for loops
test_equal([2, 4, 6], compile_and_run("a = []; for b in [1, 2, 3]; a << b * 2; end; a"))
test_equal([1, 2, 3], compile_and_run("a = []; for b, c in {:a => 1, :b => 2, :c => 3}; a << c; end; a.sort"))

# ensure blocks
test_equal(1, compile_and_run("a = 2; begin; a = 3; ensure; a = 1; end; a"))
test_equal(1, compile_and_run("$a = 2; def foo; return; ensure; $a = 1; end; foo; $a"))

# op element assign
test_equal([4, 4], compile_and_run("a = []; [a[0] ||= 4, a[0]]"))
test_equal([4, 4], compile_and_run("a = [4]; [a[0] ||= 5, a[0]]"))
test_equal([4, 4], compile_and_run("a = [1]; [a[0] += 3, a[0]]"))
test_equal([1], compile_and_run("a = {}; a[0] ||= [1]; a[0]"))
test_equal(2, compile_and_run("a = [1]; a[0] &&= 2; a[0]"))

# non-local return
test_equal(3, compile_and_run("def foo; loop {return 3}; return 4; end; foo"))

# class var declaration
test_equal(3, compile_and_run("class Foo; @@foo = 3; end"))
test_equal(3, compile_and_run("class Bar; @@bar = 3; def self.bar; @@bar; end; end; Bar.bar"))

# rescue
test_no_exception {
  test_equal(2, compile_and_run("x = begin; 1; raise; rescue; 2; end"))
  test_equal(3, compile_and_run("x = begin; 1; raise; rescue TypeError; 2; rescue; 3; end"))
  test_equal(4, compile_and_run("x = begin; 1; rescue; 2; else; 4; end"))
  test_equal(4, compile_and_run("def foo; begin; return 4; rescue; end; return 3; end; foo"))
  
  # test that $! is getting reset/cleared appropriately
  $! = nil
  test_equal(nil, compile_and_run("begin; raise; rescue; end; $!"))
  test_equal(nil, compile_and_run("1.times { begin; raise; rescue; next; end }; $!"))
  test_ok(nil != compile_and_run("begin; raise; rescue; begin; raise; rescue; end; $!; end"))
  test_ok(nil != compile_and_run("begin; raise; rescue; 1.times { begin; raise; rescue; next; end }; $!; end"))
}

# break in a while in an ensure
test_no_exception {
  test_equal(5, compile_and_run("begin; x = while true; break 5; end; ensure; end"))
}

# JRUBY-1388, Foo::Bar broke in the compiler
test_no_exception {
  test_equal(5, compile_and_run("module Foo2; end; Foo2::Foo3 = 5; Foo2::Foo3"))
}

test_equal(5, compile_and_run("def foo; yield; end; x = false; foo { break 5 if x; begin; ensure; x = true; redo; end; break 6}"))

# END block
test_no_exception { compile_and_run("END {}") }

# BEGIN block
test_equal(5, compile_and_run("BEGIN { $begin = 5 }; $begin"))

# nothing at all!
test_no_exception {
  test_equal(nil, compile_and_run(""))
}

# JRUBY-2043
test_equal(5, compile_and_run("def foo; 1.times { a, b = [], 5; a[1] = []; return b; }; end; foo"))
test_equal({"1" => 2}, compile_and_run("def foo; x = {1 => 2}; x.inject({}) do |hash, (key, value)|; hash[key.to_s] = value; hash; end; end; foo"))

# JRUBY-2246
long_src = "a = 1\n"
5000.times { long_src << "a += 1\n" }
test_equal(5001, compile_and_run(long_src))

# variable assignment of various types from loop results
test_equal(1, compile_and_run("a = while true; break 1; end; a"))
test_equal(1, compile_and_run("@a = while true; break 1; end; @a"))
test_equal(1, compile_and_run("@@a = while true; break 1; end; @@a"))
test_equal(1, compile_and_run("$a = while true; break 1; end; $a"))
test_equal(1, compile_and_run("a = until false; break 1; end; a"))
test_equal(1, compile_and_run("@a = until false; break 1; end; @a"))
test_equal(1, compile_and_run("@@a = until false; break 1; end; @@a"))
test_equal(1, compile_and_run("$a = until false; break 1; end; $a"))

# same assignments but loop is within a begin
test_equal(1, compile_and_run("a = begin; while true; break 1; end; end; a"))
test_equal(1, compile_and_run("@a = begin; while true; break 1; end; end; @a"))
test_equal(1, compile_and_run("@@a = begin; while true; break 1; end; end; @@a"))
test_equal(1, compile_and_run("$a = begin; while true; break 1; end; end; $a"))
test_equal(1, compile_and_run("a = begin; until false; break 1; end; end; a"))
test_equal(1, compile_and_run("@a = begin; until false; break 1; end; end; @a"))
test_equal(1, compile_and_run("@@a = begin; until false; break 1; end; end; @@a"))
test_equal(1, compile_and_run("$a = begin; until false; break 1; end; end; $a"))

# other contexts that require while to preserve stack
test_equal(2, compile_and_run("1 + while true; break 1; end"))
test_equal(2, compile_and_run("1 + begin; while true; break 1; end; end"))
test_equal(2, compile_and_run("1 + until false; break 1; end"))
test_equal(2, compile_and_run("1 + begin; until false; break 1; end; end"))
def foo(a); a; end
test_equal(nil, compile_and_run("foo(while false; end)"))
test_equal(nil, compile_and_run("foo(until true; end)"))

# test that 100 symbols compiles ok; that hits both types of symbol caching/creation
syms = [:a]
99.times {|i| syms << ('foo' + i.to_s).intern }
# 100 first instances of a symbol
test_equal(syms, compile_and_run(syms.inspect))
# 100 first instances and 100 second instances (caching)
test_equal([syms,syms], compile_and_run("[#{syms.inspect},#{syms.inspect}]"))

# class created using local var as superclass
test_equal('AFromLocal', compile_and_run(<<EOS))
a = Object
class AFromLocal < a
end
AFromLocal.to_s
EOS

# self should not always be true
module SelfCheck
  def self_check; if self; true; else; false; end; end
end

[NilClass, FalseClass].each {|c| c.__send__ :include, SelfCheck}
test_equal false, nil.self_check
test_equal false, false.self_check
