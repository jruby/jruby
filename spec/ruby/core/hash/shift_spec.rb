require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Hash#shift" do
  it "removes a pair from hash and return it" do
    h = { a: 1, b: 2, "c" => 3, nil => 4, [] => 5 }
    h2 = h.dup

    h.size.times do |i|
      r = h.shift
      r.should be_kind_of(Array)
      h2[r.first].should == r.last
      h.size.should == h2.size - i - 1
    end

    h.should == {}
  end

  it "returns nil from an empty hash" do
    {}.shift.should == nil
  end

  it "returns (computed) default for empty hashes" do
    Hash.new(5).shift.should == 5
    h = Hash.new { |*args| args }
    h.shift.should == [h, nil]
  end

  it "raises a RuntimeError if called on a frozen instance" do
    lambda { HashSpecs.frozen_hash.shift  }.should raise_error(RuntimeError)
    lambda { HashSpecs.empty_frozen_hash.shift }.should raise_error(RuntimeError)
  end
end
