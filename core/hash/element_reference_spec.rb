require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Hash#[]" do
  it "returns the value for key" do
    obj = mock('x')
    h = new_hash(1 => 2, 3 => 4, "foo" => "bar", obj => obj, [] => "baz")
    h[1].should == 2
    h[3].should == 4
    h["foo"].should == "bar"
    h[obj].should == obj
    h[[]].should == "baz"
  end

  it "returns nil as default default value" do
    new_hash(0 => 0)[5].should == nil
  end

  it "returns the default (immediate) value for missing keys" do
    h = new_hash 5
    h[:a].should == 5
    h[:a] = 0
    h[:a].should == 0
    h[:b].should == 5
  end

  it "calls subclass implementations of default" do
    h = HashSpecs::DefaultHash.new
    h[:nothing].should == 100
  end

  it "does not create copies of the immediate default value" do
    str = "foo"
    h = new_hash(str)
    a = h[:a]
    b = h[:b]
    a << "bar"

    a.should equal(b)
    a.should == "foobar"
    b.should == "foobar"
  end

  it "returns the default (dynamic) value for missing keys" do
    h = new_hash { |hsh, k| k.kind_of?(Numeric) ? hsh[k] = k + 2 : hsh[k] = k }
    h[1].should == 3
    h['this'].should == 'this'
    h.should == new_hash(1 => 3, 'this' => 'this')

    i = 0
    h = new_hash { |hsh, key| i += 1 }
    h[:foo].should == 1
    h[:foo].should == 2
    h[:bar].should == 3
  end

  it "does not return default values for keys with nil values" do
    h = new_hash 5
    h[:a] = nil
    h[:a].should == nil

    h = new_hash() { 5 }
    h[:a] = nil
    h[:a].should == nil
  end

  it "compares keys with eql? semantics" do
    new_hash(1.0 => "x")[1].should == nil
    new_hash(1.0 => "x")[1.0].should == "x"
    new_hash(1 => "x")[1.0].should == nil
    new_hash(1 => "x")[1].should == "x"
  end

  it "compares key via hash" do
    x = mock('0')
    x.should_receive(:hash).and_return(0)

    h = new_hash
    # 1.9 only calls #hash if the hash had at least one entry beforehand.
    h[:foo] = :bar
    h[x].should == nil
  end

  it "does not compare keys with different #hash values via #eql?" do
    x = mock('x')
    x.should_not_receive(:eql?)
    x.stub!(:hash).and_return(0)

    y = mock('y')
    y.should_not_receive(:eql?)
    y.stub!(:hash).and_return(1)

    new_hash(y => 1)[x].should == nil
  end

  it "compares keys with the same #hash value via #eql?" do
    x = mock('x')
    x.should_receive(:eql?).and_return(true)
    x.stub!(:hash).and_return(42)

    y = mock('y')
    y.should_not_receive(:eql?)
    y.stub!(:hash).and_return(42)

    new_hash(y => 1)[x].should == 1
  end

  it "finds a value via an identical key even when its #eql? isn't reflexive" do
    x = mock('x')
    x.should_receive(:hash).any_number_of_times.and_return(42)
    x.stub!(:eql?).and_return(false) # Stubbed for clarity and latitude in implementation; not actually sent by MRI.

    new_hash(x => :x)[x].should == :x
  end

  it "supports keys with private #hash method" do
    key = HashSpecs::KeyWithPrivateHash.new
    new_hash(key => 42)[key].should == 42
  end
end
