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
