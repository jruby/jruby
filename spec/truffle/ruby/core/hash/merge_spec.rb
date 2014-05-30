require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)
require File.expand_path('../shared/iteration', __FILE__)
require File.expand_path('../shared/update', __FILE__)

describe "Hash#merge" do
  it "returns a new hash by combining self with the contents of other" do
    h = new_hash(1 => :a, 2 => :b, 3 => :c).merge(:a => 1, :c => 2)
    h.should == new_hash(:c => 2, 1 => :a, 2 => :b, :a => 1, 3 => :c)
  end

  it "sets any duplicate key to the value of block if passed a block" do
    h1 = new_hash(:a => 2, :b => 1, :d => 5)
    h2 = new_hash(:a => -2, :b => 4, :c => -3)
    r = h1.merge(h2) { |k,x,y| nil }
    r.should == new_hash(:a => nil, :b => nil, :c => -3, :d => 5)

    r = h1.merge(h2) { |k,x,y| "#{k}:#{x+2*y}" }
    r.should == new_hash(:a => "a:-2", :b => "b:9", :c => -3, :d => 5)

    lambda {
      h1.merge(h2) { |k, x, y| raise(IndexError) }
    }.should raise_error(IndexError)

    r = h1.merge(h1) { |k,x,y| :x }
    r.should == new_hash(:a => :x, :b => :x, :d => :x)
  end

  it "tries to convert the passed argument to a hash using #to_hash" do
    obj = mock('{1=>2}')
    obj.should_receive(:to_hash).and_return(new_hash(1 => 2))
    new_hash(3 => 4).merge(obj).should == new_hash(1 => 2, 3 => 4)
  end

  it "does not call to_hash on hash subclasses" do
    new_hash(3 => 4).merge(HashSpecs::ToHashHash[1 => 2]).should == new_hash(1 => 2, 3 => 4)
  end

  it "returns subclass instance for subclasses" do
    HashSpecs::MyHash[1 => 2, 3 => 4].merge(new_hash(1 => 2)).should be_kind_of(HashSpecs::MyHash)
    HashSpecs::MyHash[].merge(new_hash(1 => 2)).should be_kind_of(HashSpecs::MyHash)

    new_hash(1 => 2, 3 => 4).merge(HashSpecs::MyHash[1 => 2]).class.should == hash_class
    new_hash.merge(HashSpecs::MyHash[1 => 2]).class.should == hash_class
  end

  it "processes entries with same order as each()" do
    h = new_hash(1 => 2, 3 => 4, 5 => 6, "x" => nil, nil => 5, [] => [])
    merge_pairs = []
    each_pairs = []
    h.each_pair { |k, v| each_pairs << [k, v] }
    h.merge(h) { |k, v1, v2| merge_pairs << [k, v1] }
    merge_pairs.should == each_pairs
  end

end

describe "Hash#merge!" do
  it_behaves_like(:hash_update, :merge!)

  it "does not raise an exception if changing the value of an existing key during iteration" do
      hash = {1 => 2, 3 => 4, 5 => 6}
      hash2 = {1 => :foo, 3 => :bar}
      hash.each { hash.merge!(hash2) }
      hash.should == {1 => :foo, 3 => :bar, 5 => 6}
  end
end
