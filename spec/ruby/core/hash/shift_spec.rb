require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Hash#shift" do
  it "removes a pair from hash and return it" do
    h = new_hash(:a => 1, :b => 2, "c" => 3, nil => 4, [] => 5)
    h2 = h.dup

    h.size.times do |i|
      r = h.shift
      r.should be_kind_of(Array)
      h2[r.first].should == r.last
      h.size.should == h2.size - i - 1
    end

    h.should == new_hash
  end

  it "returns nil from an empty hash " do
    new_hash.shift.should == nil
  end

  it "returns (computed) default for empty hashes" do
    new_hash(5).shift.should == 5
    h = new_hash { |*args| args }
    h.shift.should == [h, nil]
  end

  ruby_version_is "" ... "1.9" do
    it "raises a TypeError if called on a frozen instance" do
      lambda { HashSpecs.frozen_hash.shift  }.should raise_error(TypeError)
      lambda { HashSpecs.empty_frozen_hash.shift }.should raise_error(TypeError)
    end
  end

  ruby_version_is "1.9" do
    it "raises a RuntimeError if called on a frozen instance" do
      lambda { HashSpecs.frozen_hash.shift  }.should raise_error(RuntimeError)
      lambda { HashSpecs.empty_frozen_hash.shift }.should raise_error(RuntimeError)
    end
  end
end
