require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Hash.[]" do
  describe "passed zero arguments" do
    it "returns an empty hash" do
      hash_class[].should == new_hash
    end
  end

  it "creates a Hash; values can be provided as the argument list" do
    hash_class[:a, 1, :b, 2].should == new_hash(:a => 1, :b => 2)
    hash_class[].should == new_hash
    hash_class[:a, 1, :b, new_hash(:c => 2)].should ==
      new_hash(:a => 1, :b => new_hash(:c => 2))
  end

  it "creates a Hash; values can be provided as one single hash" do
    hash_class[:a => 1, :b => 2].should == new_hash(:a => 1, :b => 2)
    hash_class[new_hash(1 => 2, 3 => 4)].should == new_hash(1 => 2, 3 => 4)
    hash_class[new_hash].should == new_hash
  end

  describe "passed an array" do
    it "treats elements that are 2 element arrays as key and value" do
      hash_class[[[:a, :b], [:c, :d]]].should == new_hash(:a => :b, :c => :d)
    end

    it "treats elements that are 1 element arrays as keys with value nil" do
      hash_class[[[:a]]].should == new_hash(:a => nil)
    end
  end

  # #1000 #1385
  it "creates a Hash; values can be provided as a list of value-pairs in an array" do
    hash_class[[[:a, 1], [:b, 2]]].should == new_hash(:a => 1, :b => 2)
  end

  it "coerces a single argument which responds to #to_ary" do
    ary = mock('to_ary')
    ary.should_receive(:to_ary).and_return([[:a, :b]])

    hash_class[ary].should == new_hash(:a => :b)
  end

  it "ignores elements that are not arrays" do
    hash_class[[:a]].should == new_hash
    hash_class[[:nil]].should == new_hash
  end

  it "raises an ArgumentError for arrays of more than 2 elements" do
    lambda{ hash_class[[[:a, :b, :c]]].should == new_hash }.should raise_error(ArgumentError)
  end

  it "raises an ArgumentError when passed a list of value-invalid-pairs in an array" do
    lambda{ hash_class[[[:a, 1], [:b], 42, [:d, 2], [:e, 2, 3], []]] }.should raise_error(ArgumentError)
  end

  describe "passed a single argument which responds to #to_hash" do
    it "coerces it and returns a copy" do
      h = new_hash(:a => :b, :c => :d)
      to_hash = mock('to_hash')
      to_hash.should_receive(:to_hash).and_return(h)

      result = hash_class[to_hash]
      result.should == h
      result.should_not equal(h)
    end
  end

  it "raises an ArgumentError when passed an odd number of arguments" do
    lambda { hash_class[1, 2, 3] }.should raise_error(ArgumentError)
    lambda { hash_class[1, 2, new_hash(3 => 4)] }.should raise_error(ArgumentError)
  end

  it "calls to_hash" do
    obj = mock('x')
    def obj.to_hash() new_hash(1 => 2, 3 => 4) end
    hash_class[obj].should == new_hash(1 => 2, 3 => 4)
  end

  it "returns an instance of a subclass when passed an Array" do
    HashSpecs::MyHash[1,2,3,4].should be_an_instance_of(HashSpecs::MyHash)
  end

  it "returns instances of subclasses" do
    HashSpecs::MyHash[].should be_an_instance_of(HashSpecs::MyHash)
  end

  it "returns an instance of the class it's called on" do
    hash_class[HashSpecs::MyHash[1, 2]].class.should == hash_class
    HashSpecs::MyHash[hash_class[1, 2]].should be_an_instance_of(HashSpecs::MyHash)
  end

  it "does not call #initialize on the subclass instance" do
    HashSpecs::MyInitializerHash[hash_class[1, 2]].should be_an_instance_of(HashSpecs::MyInitializerHash)
  end

  it "removes the default_proc" do
    hash = Hash.new { |h, k| h[k] = [] }
    hash_class[hash].default_proc.should be_nil
  end
end
