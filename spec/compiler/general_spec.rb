require 'jruby'
require 'java'
require 'rspec'

is19 = RUBY_VERSION =~ /1\.9/

module CompilerTestUtils
  def compile_to_class(src, filename = nil)
    next_src_id = next_id
    node = JRuby.parse(src, filename || "testCompiler#{next_src_id}", false)
    filename = node.position.file
    classname = org.jruby.util.JavaNameMangler.mangleFilenameForClasspath(filename)
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
  
  def compile_and_run(src, filename = nil)
    cls = compile_to_class(src, filename)
  
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
    expect(compile_and_run("a = 5; a")).to eq 5
    expect(compile_and_run("a = 5.5; a")).to eq 5.5
    expect(compile_and_run("a = 'hello'; a")).to eq 'hello'
    expect(compile_and_run("a = :hello; a")).to eq :hello
    expect(compile_and_run("a = 1111111111111111111111111111; a")).to eq 1111111111111111111111111111
    expect(compile_and_run("a = [1, ['foo', :hello]]; a")).to eq([1, ['foo', :hello]])
    expect(compile_and_run("{}")).to eq({})
    expect(compile_and_run("a = {:foo => {:bar => 5.5}}; a")).to eq({:foo => {:bar => 5.5}})
    expect(compile_and_run("a = /foo/; a")).to eq(/foo/)
    expect(compile_and_run("1..2")).to eq (1..2)
  end
  
  it "compiles interpolated strings" do
    expect(compile_and_run('a = "hello#{42}"; a')).to eq('hello42')
    expect(compile_and_run('i = 1; a = "hello#{i + 42}"; a')).to eq("hello43")
  end
  
  it "compiles calls" do
    expect(compile_and_run("'bar'.capitalize")).to eq 'Bar'
    expect(compile_and_run("rand(10)")).to be_a_kind_of Fixnum
  end
  
  it "compiles branches" do
    expect(compile_and_run("a = 1; if 1 == a; 2; else; 3; end")).to eq 2
    expect(compile_and_run("a = 1; unless 1 == a; 2; else; 3; end")).to eq 3
    expect(compile_and_run("a = 1; while a < 10; a += 1; end; a")).to  eq 10
    expect(compile_and_run("a = 1; until a == 10; a += 1; end; a")).to eq 10
    expect(compile_and_run("2 if true")).to eq 2
    expect(compile_and_run("2 if false")).to be_nil
    expect(compile_and_run("2 unless true")).to be_nil
    expect(compile_and_run("2 unless false")).to eq 2
  end
  
  it "compiles while loops with no body" do
    compile_and_run("@foo = true; def flip; @foo = !@foo; end; while flip; end").should == nil
  end
  
  it "compiles boolean operators" do
    expect(compile_and_run("1 && 2")).to eq 2
    expect(compile_and_run("nil && 2")).to be_nil
    expect(compile_and_run("nil && fail")).to be_nil
    expect(compile_and_run("1 || 2")).to eq 1
    expect(compile_and_run("nil || 2")).to eq 2
    expect {compile_and_run(nil || fail)}.to raise_error(RuntimeError)
    expect(compile_and_run("1 and 2")).to eq 2
    expect(compile_and_run("1 or 2")).to eq 1
  end
  
  it "compiles begin blocks" do
    expect(compile_and_run("begin; a = 4; end; a")).to eq 4
  end
  
  it "compiles regexp matches" do
    expect(compile_and_run("/foo/ =~ 'foo'")).to eq 0
    expect(compile_and_run("'foo' =~ /foo/")).to eq 0
    expect(compile_and_run(":aaa =~ /foo/")).to (is19 ? be_nil : be_false)
  end

  it "compiles method definitions" do
    expect(compile_and_run("def foo3(arg); arg + '2'; end; foo3('baz')")).to  eq 'baz2'
    expect(compile_and_run("def self.foo3(arg); arg + '2'; end; self.foo3('baz')")).to eq 'baz2'
  end
  
  it "compiles calls with closures" do
    expect(compile_and_run("def foo2(a); a + yield.to_s; end; foo2('baz') { 4 }")).to  eq 'baz4'
    expect(compile_and_run("def foo2(a); a + yield.to_s; end; foo2('baz') {}")).to eq 'baz'
    expect(compile_and_run("def self.foo2(a); a + yield.to_s; end; self.foo2('baz') { 4 }")).to  eq 'baz4'
    expect(compile_and_run("def self.foo2(a); a + yield.to_s; end; self.foo2('baz') {}")).to eq 'baz'
  end
  
  if is19
    it "compiles strings with encoding" do
      str8bit = '"\300"'
      str8bit_result = compile_and_run(str8bit)
      expect(str8bit_result).to eq "\300"
      expect(str8bit_result.encoding).to eq Encoding::ASCII_8BIT
    end
  end
  
  it "compiles backrefs" do
    base = "'0123456789A' =~ /(1)(2)(3)(4)(5)(6)(7)(8)(9)/; "
    expect(compile_and_run(base + "$~")).to  be_a_kind_of MatchData
    expect(compile_and_run(base + "$`")).to eq '0'
    expect(compile_and_run(base + "$'")).to eq 'A'
    expect(compile_and_run(base + "$+")).to  eq '9'
    expect(compile_and_run(base + "$0")).to eq $0 # main script name, not related to matching
    expect(compile_and_run(base + "$1")).to eq '1'
    expect(compile_and_run(base + "$2")).to eq '2'
    expect(compile_and_run(base + "$3")).to eq '3'
    expect(compile_and_run(base + "$4")).to eq '4'
    expect(compile_and_run(base + "$5")).to eq '5'
    expect(compile_and_run(base + "$6")).to eq '6'
    expect(compile_and_run(base + "$7")).to eq '7'
    expect(compile_and_run(base + "$8")).to eq '8'
    expect(compile_and_run(base + "$9")).to eq '9'
  end
  
  it "compiles aliases" do
    expect(compile_and_run("alias :to_string1 :to_s; defined?(self.to_string1)")).to eq "method"
    expect(compile_and_run("alias to_string2 to_s; defined?(self.to_string2)")).to eq "method"
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
    expect(compile_and_run(blocks_code)).to eq([1,2,3,4,5,6])
  end
  
  it "compiles yield" do
    expect(compile_and_run("def foo; yield 1; end; foo {|a| a + 2}")).to eq 3
    
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
    expect(compile_and_run(yield_in_proc)).to eq 1
  end
  
  it "compiles attribute assignment" do
    expect(compile_and_run("def a=(x); 2; end; self.a = 1")).to eq 1
    expect(compile_and_run("def a; 1; end; def a=(arg); fail; end; self.a ||= 2")).to eq 1
    expect(compile_and_run("public; def a; @a; end; def a=(arg); @a = arg; 4; end; x = self.a ||= 1; [x, self.a]")).to eq([1,1])
    expect(compile_and_run("def a; nil; end; def a=(arg); fail; end; self.a &&= 2")).to  be_nil
    expect(compile_and_run("public; def a; @a; end; def a=(arg); @a = arg; end; @a = 3; x = self.a &&= 1; [x, self.a]")).to eq([1,1])
  end
  
  it "compiles lastline" do
    expect(compile_and_run("def foo; $_ = 1; bar; $_; end; def bar; $_ = 2; end; foo")).to  eq 1
  end
  
  it "compiles closure arguments" do
    expect(compile_and_run("a = 0; [1].each {|a|}; a")).to  is19 ? eq(0) : eq(1)
    expect(compile_and_run("a = 0; [1].each {|x| a = x}; a")).to  eq 1
    if !is19
      expect(compile_and_run("[1].each {|@a|}; @a")).to eq 1
      expect(compile_and_run("[[1]].each {|@a|}; @a")).to eq([1])
      expect(compile_and_run("1.times {|@@a|}; @@a")).to eq 0
      expect(compile_and_run("a = []; 1.times {|a[0]|}; a[0]")).to eq 0
      expect(compile_and_run("a = Class.new do; attr_accessor :foo; end.new; 1.times {|a.foo|}; a.foo")).to  eq 0
    end
    expect(compile_and_run("[[1,2,3]].each {|x,*y| break y}")).to  eq([2,3])
    expect(compile_and_run("1.times {|x,*y| break y}")).to  eq([])
    expect(compile_and_run("1.times {|x,*|; break x}")).to eq 0
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
    expect(compile_and_run(class_string)).to  eq 'cc1'
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
    
    expect(compile_and_run(module_string)).to  eq 'cm1'
  end
  
  it "compiles operator assignment" do
    expect(compile_and_run("class H; attr_accessor :v; end; H.new.v ||= 1")).to eq 1
    expect(compile_and_run("class H; def initialize; @v = true; end; attr_accessor :v; end; H.new.v &&= 2")).to eq 2
    expect(compile_and_run("class H; def initialize; @v = 1; end; attr_accessor :v; end; H.new.v += 3")).to eq 4
  end
  
  it "compiles optional method arguments" do
    expect(compile_and_run("def foo(a,b=1);[a,b];end;foo(1)")).to  eq([1,1])
    expect(compile_and_run("def foo(a,b=1);[a,b];end;foo(1,2)")).to eq([1,2])
    expect{compile_and_run("def foo(a,b=1);[a,b];end;foo")}.to raise_error(ArgumentError)
    expect{compile_and_run("def foo(a,b=1);[a,b];end;foo(1,2,3)")}.to raise_error(ArgumentError)
    expect(compile_and_run("def foo(a=(b=1));[a,b];end;foo")).to  eq([1,1])
    expect(compile_and_run("def foo(a=(b=1));[a,b];end;foo(2)")).to  eq([2,nil])
    expect(compile_and_run("def foo(a, b=(c=1));[a,b,c];end;foo(1)")).to eq([1,1,1])
    expect(compile_and_run("def foo(a, b=(c=1));[a,b,c];end;foo(1,2)")).to eq([1,2,nil])
    expect{compile_and_run("def foo(a, b=(c=1));[a,b,c];end;foo(1,2,3)")}.to raise_error(ArgumentError)
  end
  
  if is19
    it "compiles grouped and intra-list rest args" do
      result = compile_and_run("def foo(a, (b, *, c), d, *e, f, (g, *h, i), j); [a,b,c,d,e,f,g,h,i,j]; end; foo(1,[2,3,4],5,6,7,8,[9,10,11],12)")
      expect(result).to eq([1, 2, 4, 5, [6, 7], 8, 9, [10], 11, 12])
    end
  end
  
  it "compiles splatted values" do
    expect(compile_and_run("def foo(a,b,c);[a,b,c];end;foo(1, *[2, 3])")).to  eq([1,2,3])
    expect(compile_and_run("class Coercible1;def to_ary;[2,3];end;end; [1, *Coercible1.new]")).to  eq([1,2,3])
  end
  
  it "compiles multiple assignment" do
    expect(compile_and_run("a = nil; 1.times { a, b, @c = 1, 2, 3; a = [a, b, @c] }; a")).to  eq([1,2,3])
    expect(compile_and_run("a, (b, c) = 1; [a, b, c]")).to  eq([1,nil,nil])
    expect(compile_and_run("a, (b, c) = 1, 2; [a, b, c]")).to  eq([1,2,nil])
    expect(compile_and_run("a, (b, c) = 1, [2, 3]; [a, b, c]")).to eq([1,2,3])
    expect(compile_and_run("class Coercible2;def to_ary;[2,3]; end; end; a, (b, c) = 1, Coercible2.new; [a, b, c]")).to  eq([1,2,3])
    if is19
      result = compile_and_run("a, (b, *, c), d, *e, f, (g, *h, i), j = 1,[2,3,4],5,6,7,8,[9,10,11],12; [a,b,c,d,e,f,g,h,i,j]")
      expect(result).to  eq([1, 2, 4, 5, [6, 7], 8, 9, [10], 11, 12])
    end
  end
  
  it "compiles dynamic regexp" do
    expect(compile_and_run('"foo" =~ /#{"foo"}/')).to  eq 0
    expect(compile_and_run('ary = []; 2.times {|i| ary << ("foo0" =~ /#{"foo" + i.to_s}/o)}; ary')).to  eq([0, 0])
  end
  
  it "compiles implicit and explicit return" do
    expect(compile_and_run("def foo; 1; end; foo")).to eq 1
    expect(compile_and_run("def foo; return; end; foo")).to be_nil
    expect(compile_and_run("def foo; return 1; end; foo")).to eq 1
  end
  
  it "compiles class reopening" do
    expect(compile_and_run("class Fixnum; def x; 3; end; end; 1.x")).to  eq 3
  end
  
  it "compiles singleton method definitions" do
    expect(compile_and_run("a = 'bar'; def a.foo; 'foo'; end; a.foo")).to  eq "foo"
    expect(compile_and_run("class Fixnum; def self.foo; 'foo'; end; end; Fixnum.foo")).to eq "foo"
    expect(compile_and_run("def String.foo; 'foo'; end; String.foo")).to eq "foo"
  end

  it "compiles singleton class definitions" do
    expect(compile_and_run("a = 'bar'; class << a; def bar; 'bar'; end; end; a.bar")).to  eq "bar"
    expect(compile_and_run("class Fixnum; class << self; def bar; 'bar'; end; end; end; Fixnum.bar")).to  eq "bar"
    result = compile_and_run("class Fixnum; def self.metaclass; class << self; self; end; end; end; Fixnum.metaclass")
    expect(result).to  eq class << Fixnum; self; end
  end
  
  it "compiles loops with flow control" do
    # some loop flow control tests
    expect(compile_and_run("a = true; b = while a; a = false; break; end; b")).to  be_nil
    expect(compile_and_run("a = true; b = while a; a = false; break 1; end; b")).to  eq 1
    expect(compile_and_run("a = 0; while true; a += 1; next if a < 2; break; end; a")).to eq 2
    expect(compile_and_run("a = 0; while true; a += 1; next 1 if a < 2; break; end; a")).to eq 2
    expect(compile_and_run("a = 0; while true; a += 1; redo if a < 2; break; end; a")).to eq 2
    expect(compile_and_run("a = false; b = until a; a = true; break; end; b")).to be_nil
    expect(compile_and_run("a = false; b = until a; a = true; break 1; end; b")).to  eq 1
    expect(compile_and_run("a = 0; until false; a += 1; next if a < 2; break; end; a")).to eq 2
    expect(compile_and_run("a = 0; until false; a += 1; next 1 if a < 2; break; end; a")).to eq 2
    expect(compile_and_run("a = 0; until false; a += 1; redo if a < 2; break; end; a")).to  eq 2
    # same with evals
    expect(compile_and_run("a = true; b = while a; a = false; eval 'break'; end; b")).to be_nil
    expect(compile_and_run("a = true; b = while a; a = false; eval 'break 1'; end; b")).to eq 1
    expect(compile_and_run("a = 0; while true; a += 1; eval 'next' if a < 2; eval 'break'; end; a")).to eq 2
    expect(compile_and_run("a = 0; while true; a += 1; eval 'next 1' if a < 2; eval 'break'; end; a")).to eq 2
    expect(compile_and_run("a = 0; while true; a += 1; eval 'redo' if a < 2; eval 'break'; end; a")).to eq 2
    expect(compile_and_run("a = false; b = until a; a = true; eval 'break'; end; b")).to be_nil
    expect(compile_and_run("a = false; b = until a; a = true; eval 'break 1'; end; b")).to eq 1
    expect(compile_and_run("a = 0; until false; a += 1; eval 'next' if a < 2; eval 'break'; end; a")).to eq 2
    expect(compile_and_run("a = 0; until false; a += 1; eval 'next 1' if a < 2; eval 'break'; end; a")).to eq 2
    expect(compile_and_run("a = 0; until false; a += 1; eval 'redo' if a < 2; eval 'break'; end; a")).to eq 2
  end
  
  it "compiles loops with non-local flow control" do
    # non-local flow control with while loops
    expect(compile_and_run("a = 0; 1.times { a += 1; redo if a < 2 }; a")).to eq 2
    expect(compile_and_run("def foo(&b); while true; b.call; end; end; foo { break 3 }")).to eq 3
    # this one doesn't work normally, so I wouldn't expect it to work here yet
    #compile_and_run("a = 0; 1.times { a += 1; eval 'redo' if a < 2 }; a").should == 2
    expect(compile_and_run("def foo(&b); while true; b.call; end; end; foo { eval 'break 3' }")).to  eq 3
  end
  
  it "compiles block passing" do
    # block pass node compilation
    expect(compile_and_run("def foo; block_given?; end; p = proc {}; [foo(&nil),foo(&p)]")).to  eq([false, true])
    expect(compile_and_run("public; def foo; block_given?; end; p = proc {}; [self.foo(&nil),self.foo(&p)]")).to eq([false, true])
  end
  
  it "compiles splatted element assignment" do
    expect(compile_and_run("a = 'foo'; y = ['o']; a[*y] = 'asdf'; a")).to match "fasdfo"
  end
  
  it "compiles constant access" do
    const_code = <<-EOS
      A = 'a'; module X; B = 'b'; end; module Y; def self.go; [A, X::B, ::A]; end; end; Y.go
    EOS
    expect(compile_and_run(const_code)).to  eq(["a", "b", "a"])
  end
  
  it "compiles flip-flop" do
    # flip (taken from http://redhanded.hobix.com/inspect/hopscotchingArraysWithFlipFlops.html)
    expect(compile_and_run("s = true; (1..10).reject { true if (s = !s) .. (s) }")).to eq([1, 3, 5, 7, 9])
    expect(compile_and_run("s = true; (1..10).reject { true if (s = !s) .. (s = !s) }")).to eq([1, 4, 7, 10])
    big_flip = <<-EOS
    s = true; (1..10).inject([]) do |ary, v|; ary << [] unless (s = !s) .. (s = !s); ary.last << v; ary; end
    EOS
    expect(compile_and_run(big_flip)).to  eq([[1, 2, 3], [4, 5, 6], [7, 8, 9], [10]])
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
    expect(compile_and_run(big_triple_flip)).to  eq(expected)
  end

  it "gracefully handles named captures when there's no match" do
    expect do
      compile_and_run('/(?<a>.+)/ =~ ""')
    end.to_not raise_error
  end

  it "handles module/class opening from colon2 with non-method, non-const LHS" do
    expect do
      compile_and_run('m = Object; class m::FOOCLASS1234; end; module m::FOOMOD1234; end')
    end.to_not raise_error
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

    expect(result).to  eq([1,2,3])
  end

  if is19
    it "prepares a proper caller scope for partition/rpartition (JRUBY-6827)" do
      result = compile_and_run %q[
        def foo
          Object
          "/Users/headius/projects/jruby/tmp/perfer/examples/file_stat.rb:4:in `(root)'".rpartition(/:\d+(?:$|:in )/).first
        end

        foo]

      expect(result).to  eq '/Users/headius/projects/jruby/tmp/perfer/examples/file_stat.rb'
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

    expect(result).to  eq([nil, 1])
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

      expect(result).to  eq(["0.1+0i", "1/10"])
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
    
    expect(result).to eq 42
  end
  
  it "matches any true value for a caseless case/when with > 3 args" do
    result = compile_and_run <<-EOC
      case
      when false, false, false, true
        42
      end
    EOC
    
    expect(result).to eq 42
  end
  
  it "properly handles method-root rescue logic with returns (GH\#733)" do
    expect(compile_and_run("def foo; return 1; rescue; return 2; else; return 3; end; foo")).to eq 1
    expect(compile_and_run("def foo; 1; rescue; return 2; else; return 3; end; foo")).to eq 3
    expect(compile_and_run("def foo; raise; rescue; return 2; else; return 3; end; foo")).to eq 2
  end
  
  it "mangles filenames internally to avoid conflicting delimiters when building descriptors (GH\#961)" do
    expect(
      compile_and_run(
        "1.times { 1 }",
        "my,0.25,file:with:many|odd|delimiters.rb"
      )
    ).to eq 1
  end

  it "keeps backref local to the caller scope when calling !~" do
    obj = compile_and_run(<<-EOS)
      Class.new do
        def blank?
          "a" !~ /[^[:space:]]/
        end
      end.new
    EOS

    $~ = nil
    obj.blank?.should == false
    $~.should be_nil
  end if is19
  
  it "does a bunch of other stuff" do
    silence_warnings {
      # bug 1305, no values yielded to single-arg block assigns a null into the arg
      expect(compile_and_run("def foo; yield; end; foo {|x| x.class}")).to  eq NilClass
    }

    # ensure that invalid classes and modules raise errors
    AFixnum = 1;
    expect { compile_and_run("class AFixnum; end")}.to raise_error(TypeError)
    expect { compile_and_run("class B < AFixnum; end")}.to raise_error(TypeError)
    expect { compile_and_run("module AFixnum; end")}.to raise_error(TypeError)

    # attr assignment in multiple assign
    expect(compile_and_run("a = Object.new; class << a; attr_accessor :b; end; a.b, a.b = 'baz','bar'; a.b")).to eq "bar"
    expect(compile_and_run("a = []; a[0], a[1] = 'foo','bar'; a")).to eq(["foo", "bar"])

    # for loops
    expect(compile_and_run("a = []; for b in [1, 2, 3]; a << b * 2; end; a")).to  eq([2, 4, 6])
    expect(compile_and_run("a = []; for b, c in {:a => 1, :b => 2, :c => 3}; a << c; end; a.sort")).to  eq([1, 2, 3])

    # ensure blocks
    expect(compile_and_run("a = 2; begin; a = 3; ensure; a = 1; end; a")).to eq 1
    expect(compile_and_run("$a = 2; def foo; return; ensure; $a = 1; end; foo; $a")).to eq 1

    # op element assign
    expect(compile_and_run("a = []; [a[0] ||= 4, a[0]]")).to  eq([4, 4])
    expect(compile_and_run("a = [4]; [a[0] ||= 5, a[0]]")).to  eq([4, 4])
    expect(compile_and_run("a = [1]; [a[0] += 3, a[0]]")).to  eq([4, 4])
    expect(compile_and_run("a = {}; a[0] ||= [1]; a[0]")).to  eq([1])
    expect(compile_and_run("a = [1]; a[0] &&= 2; a[0]")).to eq 2

    # non-local return
    expect(compile_and_run("def foo; loop {return 3}; return 4; end; foo")).to eq 3

    # class var declaration
    expect(compile_and_run("class Foo; @@foo = 3; end")).to eq 3
    expect(compile_and_run("class Bar; @@bar = 3; def self.bar; @@bar; end; end; Bar.bar")).to eq 3

    # rescue
    expect(compile_and_run("x = begin; 1; raise; rescue; 2; end")).to eq 2
    expect(compile_and_run("x = begin; 1; raise; rescue TypeError; 2; rescue; 3; end")).to eq 3
    expect(compile_and_run("x = begin; 1; rescue; 2; else; 4; end")).to eq 4
    expect(compile_and_run("def foo; begin; return 4; rescue; end; return 3; end; foo")).to eq 4

    # test that $! is getting reset/cleared appropriately
    $! = nil
    expect(compile_and_run("begin; raise; rescue; end; $!")).to  be_nil
    expect(compile_and_run("1.times { begin; raise; rescue; next; end }; $!")).to  be_nil
    expect(compile_and_run("begin; raise; rescue; begin; raise; rescue; end; $!; end")).to_not be_nil
    expect(compile_and_run("begin; raise; rescue; 1.times { begin; raise; rescue; next; end }; $!; end")).to_not be_nil

    # break in a while in an ensure
    expect(compile_and_run("begin; x = while true; break 5; end; ensure; end")).to eq 5

    # JRUBY-1388, Foo::Bar broke in the compiler
    expect(compile_and_run("module Foo2; end; Foo2::Foo3 = 5; Foo2::Foo3")).to eq 5

    expect(compile_and_run("def foo; yield; end; x = false; foo { break 5 if x; begin; ensure; x = true; redo; end; break 6}")).to eq 5

    # END block
    expect { compile_and_run("END {}") }.to_not raise_error

    # BEGIN block
    expect(compile_and_run("BEGIN { $begin = 5 }; $begin")).to eq 5

    # nothing at all!
    expect(compile_and_run("")).to  be_nil

    # JRUBY-2043
    expect(compile_and_run("def foo; 1.times { a, b = [], 5; a[1] = []; return b; }; end; foo")).to eq 5
    expect(compile_and_run("def foo; x = {1 => 2}; x.inject({}) do |hash, (key, value)|; hash[key.to_s] = value; hash; end; end; foo")).to  eq({"1" => 2})

    # JRUBY-2246
    long_src = "a = 1\n"
    5000.times { long_src << "a += 1\n" }
    expect(compile_and_run(long_src)).to eq 5001

    # variable assignment of various types from loop results
    expect(compile_and_run("a = while true; break 1; end; a")).to eq 1
    expect(compile_and_run("@a = while true; break 1; end; @a")).to eq 1
    expect(compile_and_run("@@a = while true; break 1; end; @@a")).to eq 1
    expect(compile_and_run("$a = while true; break 1; end; $a")).to eq 1
    expect(compile_and_run("a = until false; break 1; end; a")).to eq 1
    expect(compile_and_run("@a = until false; break 1; end; @a")).to eq 1
    expect(compile_and_run("@@a = until false; break 1; end; @@a")).to eq 1
    expect(compile_and_run("$a = until false; break 1; end; $a")).to eq 1

    # same assignments but loop is within a begin
    expect(compile_and_run("a = begin; while true; break 1; end; end; a")).to eq 1
    expect(compile_and_run("@a = begin; while true; break 1; end; end; @a")).to eq 1
    expect(compile_and_run("@@a = begin; while true; break 1; end; end; @@a")).to eq 1
    expect(compile_and_run("$a = begin; while true; break 1; end; end; $a")).to eq 1
    expect(compile_and_run("a = begin; until false; break 1; end; end; a")).to eq 1
    expect(compile_and_run("@a = begin; until false; break 1; end; end; @a")).to eq 1
    expect(compile_and_run("@@a = begin; until false; break 1; end; end; @@a")).to eq 1
    expect(compile_and_run("$a = begin; until false; break 1; end; end; $a")).to eq 1

    # other contexts that require while to preserve stack
    expect(compile_and_run("1 + while true; break 1; end")).to eq 2
    expect(compile_and_run("1 + begin; while true; break 1; end; end")).to eq 2
    expect(compile_and_run("1 + until false; break 1; end")).to eq 2
    expect(compile_and_run("1 + begin; until false; break 1; end; end")).to eq 2
    expect(compile_and_run("def foo(a); a; end; foo(while false; end)")).to  be_nil
    expect(compile_and_run("def foo(a); a; end; foo(until true; end)")).to be_nil

    # test that 100 symbols compiles ok; that hits both types of symbol caching/creation
    syms = [:a]
    99.times {|i| syms << ('foo' + i.to_s).intern }
    # 100 first instances of a symbol
    expect(compile_and_run(syms.inspect)).to eq syms
    # 100 first instances and 100 second instances (caching)
    expect(compile_and_run("[#{syms.inspect},#{syms.inspect}]")).to eq([syms,syms])

    # class created using local var as superclass
    expect(compile_and_run(<<-EOS)).to eq 'AFromLocal'
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
    expect(compile_and_run(large_array)).to  eq(eval(large_array))
    expect(compile_and_run(large_hash)).to  eq(eval(large_hash)) unless is19 # invalid syntax in 1.9

    if is19 # block arg spreading cases
      expect(compile_and_run("def foo; a = [1]; yield a; end; foo {|a| a}")).to eq([1])
      expect(compile_and_run("x = nil; [[1]].each {|a| x = a}; x")).to  eq([1])
      expect(compile_and_run("def foo; yield [1, 2]; end; foo {|x, y| [x, y]}")).to  eq([1,2])
    end

    # non-expr case statement with return with if modified with call
    # broke in 1.9 compiler due to null "else" node pushing a nil when non-expr
    expect(compile_and_run("def foo; case 0; when 1; return 2 if self.nil?; end; return 3; end; foo")).to eq 3

    if is19 # named groups with capture
      expect(
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
      foo")).to  eq([nil,'ell', 'o', 'ell'])
    end

    if is19 # chained argscat and argspush
      expect(compile_and_run("a=[1,2];b=[4,5];[*a,3,*a,*b]")).to  eq([1,2,3,1,2,4,5])
    end

    # JRUBY-5840
    if !is19
      test = '
      nonascii = (0x80..0xff).collect{|c| c.chr }.join
      /([#{Regexp.escape(nonascii)}])/n
      '
      old_kcode = $KCODE
      $KCODE = 'UTF-8'
      expect(compile_and_run(test)).to  eq(eval(test))
      $KCODE = old_kcode
    end

    # JRUBY-5871: test that "special" args dispatch along specific-arity path
    test = '
    %w[foo bar].__send__ :to_enum, *[], &nil
    '
    expect(compile_and_run(test).map {|line| line + 'yum'}).to  eq(["fooyum", "baryum"])

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

    expect {
      JRuby5871A.new("foo", :each_byte)
    }.to_not raise_error
    
    compile_and_run "
    class JRuby5871B < #{enumerable}
      def initialize(x, y, *z)
        super(x, y, *z)
      end
    end
    "

    expect {
      JRuby5871B.new("foo", :each_byte)
    }.to_not raise_error

    class JRUBY4925
    end

    x = compile_and_run 'JRUBY4925::BLAH, a = 1, 2'
    expect(JRUBY4925::BLAH).to eq 1
    x = compile_and_run '::JRUBY4925_BLAH, a = 1, 2'
    expect(JRUBY4925_BLAH).to eq 1
  end
end
