require File.dirname(__FILE__) + '/../spec_helper'

# Language-level method behaviour


describe 'Defining methods with *' do
  it 'If * by itself is the only param, method takes any number of args that are ignored' do
    def foo(*); end;

    foo.should == nil
    foo(1, 2).should == nil
    foo(1, 2, 3, 4, :a, :b, 'c', 'd').should == nil
  end

  it 'With a parameter name, * stores all extra arguments as an Array in it' do
    def foo(*a); a; end;
    foo.should == []
    foo(1, 2).should == [1, 2]
    foo([:a]).should == [[:a]]
  end

  it 'A * param may be preceded by any number of other parameter names' do
    def foo(a, b, c, d, e, *f); [a, b, c, d, e, f]; end
    foo(1, 2, 3, 4, 5, 6, 7, 8).should == [1, 2, 3, 4, 5, [6, 7, 8]]
  end

  it 'Only one *param may appear in a parameter list' do
    should_raise(SyntaxError) { eval 'def foo(a, *b, *c); end' }
  end

  it 'The required arguments must be supplied even with a * param' do
    def foo(a, b, *c); end
    should_raise(ArgumentError) { foo 1 }
  end
end

describe "Defining a singleton method" do
  it "should work on an lvar" do
    a = "hi"
    def a.foo
      5
    end
    a.foo.should == 5
  end

  it "should work on an ivar" do
    @a = "hi"
    def @a.foo
      6
    end
    @a.foo.should == 6
  end

  it "should work on a gvar" do
    $__a__ = "hi"
    def $__a__.foo
      7
    end
    $__a__.foo.should == 7
  end

  it "should work on a cvar" do
    @@a = "hi"
    def @@a.foo
      8
    end
    @@a.foo.should == 8
  end
end
describe "Defining a method with complex default args" do
  it "should let you define a method inside a default argument" do
    def foo(x = (def foo; "hello"; end;1));x;end
    foo(42).should == 42
    foo.should == 1
    foo.should == 'hello'
  end

  it "should let you use an fcall as a default argument" do
    def foo(x = caller())
      x
    end
    foo.shift.class.should == String
  end

  it "should evaluate default arguments in the proper scope" do
    def foo(x = ($foo_self = self; nil)); end
    foo
    $foo_self.should == self
  end

  it "should support method calls on other arguments as defaults" do
    def foo(obj, width=obj.length)
      width
    end
    foo('abcde').should == 5
  end

  it "should support procs as defaults" do
    def foo(output = 'a', prc = lambda {|n| output * n})
      prc.call(5)
    end
    foo.should == 'aaaaa' 
  end
end

describe "Defining a singleton method with complex default args" do
  it "should let you define a method inside a default argument" do
    $__a = "hi"
    def $__a.foo(x = (def $__a.foo; "hello"; end;1));x;end
    $__a.foo(42).should == 42
    $__a.foo.should == 1
    $__a.foo.should == 'hello'
  end

  it "should let you use an fcall as a default argument" do
    a = "hi"
    def a.foo(x = caller())
      x
    end
    a.foo.shift.class.should == String
  end

  it "should evaluate default arguments in the proper scope" do
    a = "hi"
    def a.foo(x = ($foo_self = self; nil)); 5 ;end
    a.foo
    $foo_self.should == a
  end

  it "should support method calls on other arguments as defaults" do
    a = 'hi'
    def a.foo(obj, width=obj.length)
      width
    end
    a.foo('abcde').should == 5
  end
  
  it "should support procs as defaults" do
    a = 'hi'
    def a.foo(output = 'a', prc = lambda {|n| output * n})
      prc.call(5)
    end
    a.foo.should == 'aaaaa' 
  end
end

