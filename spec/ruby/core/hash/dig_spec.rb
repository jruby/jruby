require File.expand_path('../../../spec_helper', __FILE__)

describe "Hash#dig" do

  it "returns [] with one arg" do
    h = { 0 => false, :a => 1 }
    h.dig(:a).should == 1
    h.dig(0).should be_false
    h.dig(1).should be_nil
  end

  it "does recurse" do
    h = { foo: { bar: { baz: 1 } } }
    h.dig(:foo, :bar, :baz).should == 1
    h.dig(:foo, :baz).should be_nil
    h.dig(:bar, :baz, :foo).should be_nil
  end

  it "raises without args" do
    lambda { { the: 'borg' }.dig() }.should raise_error(ArgumentError)
  end

  it "handles type-mixed deep digging" do
    h = {}
    h[:foo] = [ { :bar => [ 1 ] }, [ obj = Object.new, 'str' ] ]
    def obj.dig(*args); [ 42 ] end

    h.dig(:foo, 0, :bar).should == [ 1 ]
    h.dig(:foo, 0, :bar, 0).should == 1
    h.dig(:foo, 0, :bar, 0, 0).should be_nil
    h.dig(:foo, 1, 1).should == 'str'
    h.dig(:foo, 1, 1, 0).should be_nil
    # MRI does not recurse values returned from `obj.dig`
    h.dig(:foo, 1, 0, 0).should == [ 42 ]
    h.dig(:foo, 1, 0, 0, 10).should == [ 42 ]
  end

end
