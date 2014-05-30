require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Hash#fetch" do
  it "returns the value for key" do
    new_hash(:a => 1, :b => -1).fetch(:b).should == -1
  end

  ruby_version_is ""..."1.9" do
    it "raises an IndexError if key is not found" do
      lambda { new_hash.fetch(:a)       }.should raise_error(IndexError)
      lambda { new_hash(5).fetch(:a)    }.should raise_error(IndexError)
      lambda { new_hash { 5 }.fetch(:a) }.should raise_error(IndexError)
    end
  end

  ruby_version_is "1.9" do
    it "raises an KeyError if key is not found" do
      lambda { new_hash.fetch(:a)       }.should raise_error(KeyError)
      lambda { new_hash(5).fetch(:a)    }.should raise_error(KeyError)
      lambda { new_hash { 5 }.fetch(:a) }.should raise_error(KeyError)
    end
  end

  it "returns default if key is not found when passed a default" do
    new_hash.fetch(:a, nil).should == nil
    new_hash.fetch(:a, 'not here!').should == "not here!"
    new_hash(:a => nil).fetch(:a, 'not here!').should == nil
  end

  it "returns value of block if key is not found when passed a block" do
    new_hash.fetch('a') { |k| k + '!' }.should == "a!"
  end

  it "gives precedence to the default block over the default argument when passed both" do
    new_hash.fetch(9, :foo) { |i| i * i }.should == 81
  end

  it "raises an ArgumentError when not passed one or two arguments" do
    lambda { new_hash.fetch()        }.should raise_error(ArgumentError)
    lambda { new_hash.fetch(1, 2, 3) }.should raise_error(ArgumentError)
  end
end
