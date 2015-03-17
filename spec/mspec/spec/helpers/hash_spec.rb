require 'spec_helper'
require 'mspec/guards'
require 'mspec/helpers'

describe Object, "#hash_class" do
  it "returns the Hash class" do
    hash_class.should == Hash
  end
end

describe Object, "#new_hash" do
  it "returns a default hash" do
    new_hash.should == {}
  end

  it "returns a hash having a default value" do
    hash = new_hash(5)
    hash[:none].should == 5
  end

  it "returns a hash having a default proc" do
    hash = new_hash { |h, k| h[k] = :default }
    hash[:none].should == :default
  end

  it "returns a hash constructed from keys and values" do
    new_hash({:a => 1, :b => 2}).should == { :a => 1, :b => 2 }
    new_hash(1 => 2, 3 => 4).should == { 1 => 2, 3 => 4 }
    new_hash(1, 2, 3, 4).should == { 1 => 2, 3 => 4 }
  end
end
