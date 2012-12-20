require 'jruby'
require 'java'
require 'rspec'

is19 = RUBY_VERSION =~ /1\.9/

module CompilerTestUtils
  def compile_to_class(src)
    next_src_id = next_id
    node = JRuby.parse(src, "testCompiler#{next_src_id}", false)
    filename = node.position.file
    classname = filename.sub("/", ".").sub("\\", ".").sub(".rb", "")
    inspector = ASTInspector.new
    inspector.inspect(node)
    context = StandardASMCompiler.new(classname, filename)
    compiler = ASTCompiler.new
    compiler.compileRoot(node, context, inspector)

    context.loadClass(JRuby.runtime.getJRubyClassLoader)
  end

  def next_id
    $compiler_spec_id ||= 0
    $compiler_spec_id += 1
  end
  
  def silence_warnings
    verb = $VERBOSE
    $VERBOSE = nil
    yield
  ensure
    $VERBOSE = verb
  end
  
  def compile_and_run(src)
    cls = compile_to_class(src)
  
    cls.new_instance.load(JRuby.runtime.current_context, JRuby.runtime.top_self, IRubyObject[0].new, Block::NULL_BLOCK)
  end
end

describe "JRuby's compiler" do
  include CompilerTestUtils
  
  StandardASMCompiler = org.jruby.compiler.impl.StandardASMCompiler
  ASTCompiler = is19 ? org.jruby.compiler.ASTCompiler19 : org.jruby.compiler.ASTCompiler
  ASTInspector = org.jruby.compiler.ASTInspector
  Block = org.jruby.runtime.Block
  IRubyObject = org.jruby.runtime.builtin.IRubyObject

  it "assigns literal values to locals" do
    compile_and_run("a = 5; a").should == 5
    compile_and_run("a = 5.5; a").should == 5.5
    compile_and_run("a = 'hello'; a").should == 'hello'
    compile_and_run("a = :hello; a").should == :hello
    compile_and_run("a = 1111111111111111111111111111; a").should == 1111111111111111111111111111
    compile_and_run("a = [1, ['foo', :hello]]; a").should == [1, ['foo', :hello]]
    compile_and_run("{}").should == {}
    compile_and_run("a = {:foo => {:bar => 5.5}}; a").should == {:foo => {:bar => 5.5}}
    compile_and_run("a = /foo/; a").should == /foo/
    compile_and_run("1..2").should == (1..2)
  end
  
  it "compiles interpolated strings" do
    compile_and_run('a = "hello#{42}"; a').should == 'hello42'
    compile_and_run('i = 1; a = "hello#{i + 42}"; a').should == "hello43"
  end
  
  it "compiles calls" do
    compile_and_run("'bar'.capitalize").should == 'Bar'
    compile_and_run("rand(10)").class.should == Fixnum
  end
  
  it "compiles branches" do
    compile_and_run("a = 1; if 1 == a; 2; else; 3; end").should == 2
    compile_and_run("a = 1; unless 1 == a; 2; else; 3; end").should == 3
    compile_and_run("a = 1; while a < 10; a += 1; end; a").should == 10
    compile_and_run("a = 1; until a == 10; a += 1; end; a").should == 10
    compile_and_run("2 if true").should == 2
    compile_and_run("2 if false").should == nil
    compile_and_run("2 unless true").should == nil
    compile_and_run("2 unless false").should == 2
  end
  
  it "compiles while loops with no body" do
    compile_and_run("@foo = true; def flip; @foo = !@foo; end; while flip; end").should == nil
  end
  
  it "compiles boolean operators" do
    compile_and_run("1 && 2").should == 2
    compile_and_run("nil && 2").should == nil
    compile_and_run("nil && fail").should == nil
    compile_and_run("1 || 2").should == 1
    compile_and_run("nil || 2").should == 2
    lambda {compile_and_run(nil || fail)}.should raise_error(RuntimeError)
    compile_and_run("1 and 2").should == 2
    compile_and_run("1 or 2").should == 1
  end
  
  it "compiles begin blocks" do
    compile_and_run("begin; a = 4; end; a").should == 4
  end
  
  it "compiles regexp matches" do
    compile_and_run("/foo/ =~ 'foo'").should == 0
    compile_and_run("'foo' =~ /foo/").should == 0
    compile_and_run(":aaa =~ /foo/").should == (is19 ? nil : false)
  end

  it "compiles method definitions" do
    compile_and_run("def foo3(arg); arg + '2'; end; foo3('baz')").should == 'baz2'
    compile_and_run("def self.foo3(arg); arg + '2'; end; self.foo3('baz')").should == 'baz2'
  end
  
  it "compiles calls with closures" do
    compile_and_run("def foo2(a); a + yield.to_s; end; foo2('baz') { 4 }").should == 'baz4'
    compile_and_run("def foo2(a); a + yield.to_s; end; foo2('baz') {}").should == 'baz'
    compile_and_run("def self.foo2(a); a + yield.to_s; end; self.foo2('baz') { 4 }").should == 'baz4'
    compile_and_run("def self.foo2(a); a + yield.to_s; end; self.foo2('baz') {}").should == 'baz'
  end
  
  if is19
    it "compiles strings with encoding" do
      str8bit = '"\300"'
      str8bit_result = compile_and_run(str8bit)
      str8bit_result.should == "\300"
      str8bit_result.encoding.should == Encoding::ASCII_8BIT
    end
  end
  
  it "compiles backrefs" do
    base = "'0123456789A' =~ /(1)(2)(3)(4)(5)(6)(7)(8)(9)/; "
    compile_and_run(base + "$~").class.should == MatchData
    compile_and_run(base + "$`").should == '0'
    compile_and_run(base + "$'").should == 'A'
    compile_and_run(base + "$+").should == '9'
    compile_and_run(base + "$0").should == $0 # main script name, not related to matching
    compile_and_run(base + "$1").should == '1'
    compile_and_run(base + "$2").should == '2'
    compile_and_run(base + "$3").should == '3'
    compile_and_run(base + "$4").should == '4'
    compile_and_run(base + "$5").should == '5'
    compile_and_run(base + "$6").should == '6'
    compile_and_run(base + "$7").should == '7'
    compile_and_run(base + "$8").should == '8'
    compile_and_run(base + "$9").should == '9'
  end
  
  it "compiles aliases" do
    compile_and_run("alias :to_string1 :to_s; defined?(self.to_string1)").should == "method"
    compile_and_run("alias to_string2 to_s; defined?(self.to_string2)").should == "method"
  end
  
  it "compiles block-local variables" do
    blocks_code = <<-EOS
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
      arr
      EOS
    compile_and_run(blocks_code).should == [1,2,3,4,5,6]
  end
  
  it "compiles yield" do
    compile_and_run("def foo; yield 1; end; foo {|a| a + 2}").should == 3
    
    yield_in_block = <<-EOS
      def foo
        bar { yield }
      end
      def bar
        yield
      end
      foo { 1 }
      EOS
    compile_and_run(yield_in_block)
    
    yield_in_proc = <<-EOS
      def foo
        proc { yield }
      end
      p = foo { 1 }
      p.call
      EOS
    compile_and_run(yield_in_proc).should == 1
  end
  
  it "compiles attribute assignment" do
    compile_and_run("def a=(x); 2; end; self.a = 1").should == 1
    compile_and_run("def a; 1; end; def a=(arg); fail; end; self.a ||= 2").should == 1
    compile_and_run("public; def a; @a; end; def a=(arg); @a = arg; 4; end; x = self.a ||= 1; [x, self.a]").should == [1,1]
    compile_and_run("def a; nil; end; def a=(arg); fail; end; self.a &&= 2").should == nil
    compile_and_run("public; def a; @a; end; def a=(arg); @a = arg; end; @a = 3; x = self.a &&= 1; [x, self.a]").should == [1,1]
  end
  
  it "compiles lastline" do
    compile_and_run("def foo; $_ = 1; bar; $_; end; def bar; $_ = 2; end; foo").should == 1
  end
  
  it "compiles closure arguments" do
    compile_and_run("a = 0; [1].each {|a|}; a").should == (is19 ? 0 : 1)
    compile_and_run("a = 0; [1].each {|x| a = x}; a").should == 1
    if !is19
      compile_and_run("[1].each {|@a|}; @a").should == 1
      compile_and_run("[[1]].each {|@a|}; @a").should == [1]
      compile_and_run("1.times {|@@a|}; @@a").should == 0
      compile_and_run("a = []; 1.times {|a[0]|}; a[0]").should == 0
      compile_and_run("a = Class.new do; attr_accessor :foo; end.new; 1.times {|a.foo|}; a.foo").should == 0
    end
    compile_and_run("[[1,2,3]].each {|x,*y| break y}").should == [2,3]
    compile_and_run("1.times {|x,*y| break y}").should == []
    compile_and_run("1.times {|x,*|; break x}").should == 0
  end
  
  it "compiles class definitions" do
    class_string = <<-EOS
      class CompiledClass1
        def foo
          "cc1"
        end
      end
      CompiledClass1.new.foo
      EOS
    compile_and_run(class_string).should == 'cc1'
  end
  
  it "compiles module definitions" do
    module_string = <<-EOS
      module CompiledModule1
        def self.bar
          "cm1"
        end
      end
      CompiledModule1.bar
    EOS
    
    compile_and_run(module_string).should == 'cm1'
  end
  
  it "compiles operator assignment" do
    compile_and_run("class H; attr_accessor :v; end; H.new.v ||= 1").should == 1
    compile_and_run("class H; def initialize; @v = true; end; attr_accessor :v; end; H.new.v &&= 2").should == 2
    compile_and_run("class H; def initialize; @v = 1; end; attr_accessor :v; end; H.new.v += 3").should == 4
  end
  
  it "compiles optional method arguments" do
    compile_and_run("def foo(a,b=1);[a,b];end;foo(1)").should == [1,1]
    compile_and_run("def foo(a,b=1);[a,b];end;foo(1,2)").should == [1,2]
    lambda{compile_and_run("def foo(a,b=1);[a,b];end;foo")}.should raise_error(ArgumentError)
    lambda{compile_and_run("def foo(a,b=1);[a,b];end;foo(1,2,3)")}.should raise_error(ArgumentError)
    compile_and_run("def foo(a=(b=1));[a,b];end;foo").should == [1,1]
    compile_and_run("def foo(a=(b=1));[a,b];end;foo(2)").should == [2,nil]
    compile_and_run("def foo(a, b=(c=1));[a,b,c];end;foo(1)").should == [1,1,1]
    compile_and_run("def foo(a, b=(c=1));[a,b,c];end;foo(1,2)").should == [1,2,nil]
    lambda{compile_and_run("def foo(a, b=(c=1));[a,b,c];end;foo(1,2,3)")}.should raise_error(ArgumentError)
  end
  
  if is19
    it "compiles grouped and intra-list rest args" do
      result = compile_and_run("def foo(a, (b, *, c), d, *e, f, (g, *h, i), j); [a,b,c,d,e,f,g,h,i,j]; end; foo(1,[2,3,4],5,6,7,8,[9,10,11],12)")
      result.should == [1, 2, 4, 5, [6, 7], 8, 9, [10], 11, 12]
    end
  end
  
  it "compiles splatted values" do
    compile_and_run("def foo(a,b,c);[a,b,c];end;foo(1, *[2, 3])").should == [1,2,3]
    compile_and_run("class Coercible1;def to_ary;[2,3];end;end; [1, *Coercible1.new]").should == [1,2,3]
  end
  
  it "compiles multiple assignment" do
    compile_and_run("a = nil; 1.times { a, b, @c = 1, 2, 3; a = [a, b, @c] }; a").should == [1,2,3]
    compile_and_run("a, (b, c) = 1; [a, b, c]").should == [1,nil,nil]
    compile_and_run("a, (b, c) = 1, 2; [a, b, c]").should == [1,2,nil]
    compile_and_run("a, (b, c) = 1, [2, 3]; [a, b, c]").should == [1,2,3]
    compile_and_run("class Coercible2;def to_ary;[2,3]; end; end; a, (b, c) = 1, Coercible2.new; [a, b, c]").should == [1,2,3]
    if is19
      result = compile_and_run("a, (b, *, c), d, *e, f, (g, *h, i), j = 1,[2,3,4],5,6,7,8,[9,10,11],12; [a,b,c,d,e,f,g,h,i,j]")
      result.should == [1, 2, 4, 5, [6, 7], 8, 9, [10], 11, 12]
    end
  end
  
  it "compiles dynamic regexp" do
    compile_and_run('"foo" =~ /#{"foo"}/').should == 0
    compile_and_run('ary = []; 2.times {|i| ary << ("foo0" =~ /#{"foo" + i.to_s}/o)}; ary').should == [0, 0]
  end
  
  it "compiles implicit and explicit return" do
    compile_and_run("def foo; 1; end; foo").should == 1
    compile_and_run("def foo; return; end; foo").should == nil
    compile_and_run("def foo; return 1; end; foo").should == 1
  end
  
  it "compiles class reopening" do
    compile_and_run("class Fixnum; def x; 3; end; end; 1.x").should == 3
  end
  
  it "compiles singleton method definitions" do
    compile_and_run("a = 'bar'; def a.foo; 'foo'; end; a.foo").should == "foo"
    compile_and_run("class Fixnum; def self.foo; 'foo'; end; end; Fixnum.foo").should == "foo"
    compile_and_run("def String.foo; 'foo'; end; String.foo").should == "foo"
  end

  it "compiles singleton class definitions" do
    compile_and_run("a = 'bar'; class << a; def bar; 'bar'; end; end; a.bar").should == "bar"
    compile_and_run("class Fixnum; class << self; def bar; 'bar'; end; end; end; Fixnum.bar").should == "bar"
    result = compile_and_run("class Fixnum; def self.metaclass; class << self; self; end; end; end; Fixnum.metaclass")
    result.should == class << Fixnum; self; end
  end
  
  it "compiles loops with flow control" do
    # some loop flow control tests
    compile_and_run("a = true; b = while a; a = false; break; end; b").should == nil
    compile_and_run("a = true; b = while a; a = false; break 1; end; b").should == 1
    compile_and_run("a = 0; while true; a += 1; next if a < 2; break; end; a").should == 2
    compile_and_run("a = 0; while true; a += 1; next 1 if a < 2; break; end; a").should == 2
    compile_and_run("a = 0; while true; a += 1; redo if a < 2; break; end; a").should == 2
    compile_and_run("a = false; b = until a; a = true; break; end; b").should == nil
    compile_and_run("a = false; b = until a; a = true; break 1; end; b").should == 1
    compile_and_run("a = 0; until false; a += 1; next if a < 2; break; end; a").should == 2
    compile_and_run("a = 0; until false; a += 1; next 1 if a < 2; break; end; a").should == 2
    compile_and_run("a = 0; until false; a += 1; redo if a < 2; break; end; a").should == 2
    # same with evals
    compile_and_run("a = true; b = while a; a = false; eval 'break'; end; b").should == nil
    compile_and_run("a = true; b = while a; a = false; eval 'break 1'; end; b").should == 1
    compile_and_run("a = 0; while true; a += 1; eval 'next' if a < 2; eval 'break'; end; a").should == 2
    compile_and_run("a = 0; while true; a += 1; eval 'next 1' if a < 2; eval 'break'; end; a").should == 2
    compile_and_run("a = 0; while true; a += 1; eval 'redo' if a < 2; eval 'break'; end; a").should == 2
    compile_and_run("a = false; b = until a; a = true; eval 'break'; end; b").should == nil
    compile_and_run("a = false; b = until a; a = true; eval 'break 1'; end; b").should == 1
    compile_and_run("a = 0; until false; a += 1; eval 'next' if a < 2; eval 'break'; end; a").should == 2
    compile_and_run("a = 0; until false; a += 1; eval 'next 1' if a < 2; eval 'break'; end; a").should == 2
    compile_and_run("a = 0; until false; a += 1; eval 'redo' if a < 2; eval 'break'; end; a").should == 2
  end
  
  it "compiles loops with non-local flow control" do
    # non-local flow control with while loops
    compile_and_run("a = 0; 1.times { a += 1; redo if a < 2 }; a").should == 2
    compile_and_run("def foo(&b); while true; b.call; end; end; foo { break 3 }").should == 3
    # this one doesn't work normally, so I wouldn't expect it to work here yet
    #compile_and_run("a = 0; 1.times { a += 1; eval 'redo' if a < 2 }; a").should == 2
    compile_and_run("def foo(&b); while true; b.call; end; end; foo { eval 'break 3' }").should == 3
  end
  
  it "compiles block passing" do
    # block pass node compilation
    compile_and_run("def foo; block_given?; end; p = proc {}; [foo(&nil),foo(&p)]").should == [false, true]
    compile_and_run("public; def foo; block_given?; end; p = proc {}; [self.foo(&nil),self.foo(&p)]").should == [false, true]
  end
  
  it "compiles splatted element assignment" do
    compile_and_run("a = 'foo'; y = ['o']; a[*y] = 'asdf'; a").should == "fasdfo"
  end
  
  it "compiles constant access" do
    const_code = <<-EOS
      A = 'a'; module X; B = 'b'; end; module Y; def self.go; [A, X::B, ::A]; end; end; Y.go
    EOS
    compile_and_run(const_code).should == ["a", "b", "a"]
  end
  
  it "compiles flip-flop" do
    # flip (taken from http://redhanded.hobix.com/inspect/hopscotchingArraysWithFlipFlops.html)
    compile_and_run("s = true; (1..10).reject { true if (s = !s) .. (s) }").should == [1, 3, 5, 7, 9]
    compile_and_run("s = true; (1..10).reject { true if (s = !s) .. (s = !s) }").should == [1, 4, 7, 10]
    big_flip = <<-EOS
    s = true; (1..10).inject([]) do |ary, v|; ary << [] unless (s = !s) .. (s = !s); ary.last << v; ary; end
    EOS
    compile_and_run(big_flip).should == [[1, 2, 3], [4, 5, 6], [7, 8, 9], [10]]
    big_triple_flip = <<-EOS
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
    compile_and_run(big_triple_flip).should == expected
  end

  it "gracefully handles named captures when there's no match" do
    lambda do
      compile_and_run('/(?<a>.+)/ =~ ""')
    end.should_not raise_error
  end

  it "handles module/class opening from colon2 with non-method, non-const LHS" do
    lambda do
      compile_and_run('m = Object; class m::FOOCLASS1234; end; module m::FOOMOD1234; end')
    end.should_not raise_error
  end

  it "properly handles non-local flow for a loop inside an ensure (JRUBY-6836)" do
    ary = []
    result = compile_and_run '
      def main
        ary = []
        while true
          begin
            break
          ensure
            ary << 1
          end
        end
        ary << 2
      ensure
        ary << 3
      end

      main'

    result.should == [1,2,3]
  end

  if is19
    it "prepares a proper caller scope for partition/rpartition (JRUBY-6827)" do
      result = compile_and_run %q[
        def foo
          Object
          "/Users/headius/projects/jruby/tmp/perfer/examples/file_stat.rb:4:in `(root)'".rpartition(/:\d+(?:$|:in )/).first
        end

        foo]

      result.should == '/Users/headius/projects/jruby/tmp/perfer/examples/file_stat.rb'
    end
  end

  it "handles attr accessors for unassigned vars properly" do
    # under invokedynamic, we were caching the "dummy" accessor that never saw any value
    result = compile_and_run <<-EOC
class AttrAccessorUnassigned
  attr_accessor :foo
end

obj = AttrAccessorUnassigned.new
ary = []
2.times { ary << obj.foo; obj.foo = 1}
ary
    EOC

    result.should == [nil, 1]
  end

  if is19
    it "does not break String#to_r and to_c" do
      # This is structured to cause a "dummy" scope because of the String constant
      # This caused to_r and to_c to fail since that scope always returns nil
      result = compile_and_run <<-EOC
      def foo
        [String.new("0.1".to_c.to_s), String.new("0.1".to_r.to_s)]
      end
      foo
      EOC

      result.should == ["0.1+0i", "1/10"]
    end
  end
  
  it "handles 0-4 arg and splatted whens in a caseless case/when" do
    result = compile_and_run <<-EOC
      case
      when false
        fail
      when false, false
        fail
      when false, false, false
        fail
      when false, false, false, false
        fail
      when *[false, false, false, false]
      else
        42
      end
    EOC
    
    result.should == 42
  end
  
  it "matches any true value for a caseless case/when with > 3 args" do
    result = compile_and_run <<-EOC
      case
      when false, false, false, true
        42
      end
    EOC
    
    result.should == 42
  end
  
  it "does a bunch of other stuff" do
    silence_warnings {
      # bug 1305, no values yielded to single-arg block assigns a null into the arg
      compile_and_run("def foo; yield; end; foo {|x| x.class}").should == NilClass
    }

    # ensure that invalid classes and modules raise errors
    AFixnum = 1;
    lambda { compile_and_run("class AFixnum; end")}.should raise_error(TypeError)
    lambda { compile_and_run("class B < AFixnum; end")}.should raise_error(TypeError)
    lambda { compile_and_run("module AFixnum; end")}.should raise_error(TypeError)

    # attr assignment in multiple assign
    compile_and_run("a = Object.new; class << a; attr_accessor :b; end; a.b, a.b = 'baz','bar'; a.b").should == "bar"
    compile_and_run("a = []; a[0], a[1] = 'foo','bar'; a").should == ["foo", "bar"]

    # for loops
    compile_and_run("a = []; for b in [1, 2, 3]; a << b * 2; end; a").should == [2, 4, 6]
    compile_and_run("a = []; for b, c in {:a => 1, :b => 2, :c => 3}; a << c; end; a.sort").should == [1, 2, 3]

    # ensure blocks
    compile_and_run("a = 2; begin; a = 3; ensure; a = 1; end; a").should == 1
    compile_and_run("$a = 2; def foo; return; ensure; $a = 1; end; foo; $a").should == 1

    # op element assign
    compile_and_run("a = []; [a[0] ||= 4, a[0]]").should == [4, 4]
    compile_and_run("a = [4]; [a[0] ||= 5, a[0]]").should == [4, 4]
    compile_and_run("a = [1]; [a[0] += 3, a[0]]").should == [4, 4]
    compile_and_run("a = {}; a[0] ||= [1]; a[0]").should == [1]
    compile_and_run("a = [1]; a[0] &&= 2; a[0]").should == 2

    # non-local return
    compile_and_run("def foo; loop {return 3}; return 4; end; foo").should == 3

    # class var declaration
    compile_and_run("class Foo; @@foo = 3; end").should == 3
    compile_and_run("class Bar; @@bar = 3; def self.bar; @@bar; end; end; Bar.bar").should == 3

    # rescue
    compile_and_run("x = begin; 1; raise; rescue; 2; end").should == 2
    compile_and_run("x = begin; 1; raise; rescue TypeError; 2; rescue; 3; end").should == 3
    compile_and_run("x = begin; 1; rescue; 2; else; 4; end").should == 4
    compile_and_run("def foo; begin; return 4; rescue; end; return 3; end; foo").should == 4

    # test that $! is getting reset/cleared appropriately
    $! = nil
    compile_and_run("begin; raise; rescue; end; $!").should == nil
    compile_and_run("1.times { begin; raise; rescue; next; end }; $!").should == nil
    compile_and_run("begin; raise; rescue; begin; raise; rescue; end; $!; end").should_not == nil
    compile_and_run("begin; raise; rescue; 1.times { begin; raise; rescue; next; end }; $!; end").should_not == nil

    # break in a while in an ensure
    compile_and_run("begin; x = while true; break 5; end; ensure; end").should == 5

    # JRUBY-1388, Foo::Bar broke in the compiler
    compile_and_run("module Foo2; end; Foo2::Foo3 = 5; Foo2::Foo3").should == 5

    compile_and_run("def foo; yield; end; x = false; foo { break 5 if x; begin; ensure; x = true; redo; end; break 6}").should == 5

    # END block
    lambda { compile_and_run("END {}") }.should_not raise_error

    # BEGIN block
    compile_and_run("BEGIN { $begin = 5 }; $begin").should == 5

    # nothing at all!
    compile_and_run("").should == nil

    # JRUBY-2043
    compile_and_run("def foo; 1.times { a, b = [], 5; a[1] = []; return b; }; end; foo").should == 5
    compile_and_run("def foo; x = {1 => 2}; x.inject({}) do |hash, (key, value)|; hash[key.to_s] = value; hash; end; end; foo").should == {"1" => 2}

    # JRUBY-2246
    long_src = "a = 1\n"
    5000.times { long_src << "a += 1\n" }
    compile_and_run(long_src).should == 5001

    # variable assignment of various types from loop results
    compile_and_run("a = while true; break 1; end; a").should == 1
    compile_and_run("@a = while true; break 1; end; @a").should == 1
    compile_and_run("@@a = while true; break 1; end; @@a").should == 1
    compile_and_run("$a = while true; break 1; end; $a").should == 1
    compile_and_run("a = until false; break 1; end; a").should == 1
    compile_and_run("@a = until false; break 1; end; @a").should == 1
    compile_and_run("@@a = until false; break 1; end; @@a").should == 1
    compile_and_run("$a = until false; break 1; end; $a").should == 1

    # same assignments but loop is within a begin
    compile_and_run("a = begin; while true; break 1; end; end; a").should == 1
    compile_and_run("@a = begin; while true; break 1; end; end; @a").should == 1
    compile_and_run("@@a = begin; while true; break 1; end; end; @@a").should == 1
    compile_and_run("$a = begin; while true; break 1; end; end; $a").should == 1
    compile_and_run("a = begin; until false; break 1; end; end; a").should == 1
    compile_and_run("@a = begin; until false; break 1; end; end; @a").should == 1
    compile_and_run("@@a = begin; until false; break 1; end; end; @@a").should == 1
    compile_and_run("$a = begin; until false; break 1; end; end; $a").should == 1

    # other contexts that require while to preserve stack
    compile_and_run("1 + while true; break 1; end").should == 2
    compile_and_run("1 + begin; while true; break 1; end; end").should == 2
    compile_and_run("1 + until false; break 1; end").should == 2
    compile_and_run("1 + begin; until false; break 1; end; end").should == 2
    compile_and_run("def foo(a); a; end; foo(while false; end)").should == nil
    compile_and_run("def foo(a); a; end; foo(until true; end)").should == nil

    # test that 100 symbols compiles ok; that hits both types of symbol caching/creation
    syms = [:a]
    99.times {|i| syms << ('foo' + i.to_s).intern }
    # 100 first instances of a symbol
    compile_and_run(syms.inspect).should == syms
    # 100 first instances and 100 second instances (caching)
    compile_and_run("[#{syms.inspect},#{syms.inspect}]").should == [syms,syms]

    # class created using local var as superclass
    compile_and_run(<<-EOS).should == 'AFromLocal'
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
    nil.self_check.should == false
    false.self_check.should == false

    # JRUBY-4757 and JRUBY-2621: can't compile large array/hash
    large_array = (1..10000).to_a.inspect
    large_hash = large_array.clone
    large_hash.gsub!('[', '{')
    large_hash.gsub!(']', '}')
    compile_and_run(large_array).should == eval(large_array)
    compile_and_run(large_hash).should == eval(large_hash) unless is19 # invalid syntax in 1.9

    if is19 # block arg spreading cases
      compile_and_run("def foo; a = [1]; yield a; end; foo {|a| a}").should == [1]
      compile_and_run("x = nil; [[1]].each {|a| x = a}; x").should == [1]
      compile_and_run("def foo; yield [1, 2]; end; foo {|x, y| [x, y]}").should == [1,2]
    end

    # non-expr case statement with return with if modified with call
    # broke in 1.9 compiler due to null "else" node pushing a nil when non-expr
    compile_and_run("def foo; case 0; when 1; return 2 if self.nil?; end; return 3; end; foo").should == 3

    if is19 # named groups with capture
      compile_and_run("
      def foo
        ary = []
        a = nil
        b = nil
        1.times {
          /(?<b>ell)(?<c>o)/ =~ 'hello'
          ary << a
          ary << b
          ary << c
        }
        ary << b
        ary
      end
      foo").should == [nil,'ell', 'o', 'ell']
    end

    if is19 # chained argscat and argspush
      compile_and_run("a=[1,2];b=[4,5];[*a,3,*a,*b]").should == [1,2,3,1,2,4,5]
    end

    # JRUBY-5840
    if !is19
      test = '
      nonascii = (0x80..0xff).collect{|c| c.chr }.join
      /([#{Regexp.escape(nonascii)}])/n
      '
      old_kcode = $KCODE
      $KCODE = 'UTF-8'
      compile_and_run(test).should == eval(test)
      $KCODE = old_kcode
    end

    # JRUBY-5871: test that "special" args dispatch along specific-arity path
    test = '
    %w[foo bar].__send__ :to_enum, *[], &nil
    '
    compile_and_run(test).map {|line| line + 'yum'}.should == ["fooyum", "baryum"]

    # These two cases triggered ArgumentError when Enumerator was fixed to enforce
    # 3 required along its varargs path. Testing both here to ensure super/zsuper
    # also dispatch along arity-specific paths as appropriate
    enumerable = is19 ? "Enumerator" : "Enumerable::Enumerator"
    compile_and_run "
    class JRuby5871A < #{enumerable}
      def initialize(x, y, *z)
        super
      end
    end
    "

    lambda {
      JRuby5871A.new("foo", :each_byte)
    }.should_not raise_error
    
    compile_and_run "
    class JRuby5871B < #{enumerable}
      def initialize(x, y, *z)
        super(x, y, *z)
      end
    end
    "

    lambda {
      JRuby5871B.new("foo", :each_byte)
    }.should_not raise_error

    class JRUBY4925
    end

    x = compile_and_run 'JRUBY4925::BLAH, a = 1, 2'
    JRUBY4925::BLAH.should == 1
    x = compile_and_run '::JRUBY4925_BLAH, a = 1, 2'
    JRUBY4925_BLAH.should == 1
  end
end
