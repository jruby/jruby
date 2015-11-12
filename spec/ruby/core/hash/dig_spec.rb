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

end
