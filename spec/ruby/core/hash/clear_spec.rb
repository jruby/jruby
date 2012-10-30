require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Hash#clear" do
  it "removes all key, value pairs" do
    h = new_hash(1 => 2, 3 => 4)
    h.clear.should equal(h)
    h.should == new_hash
  end

  it "does not remove default values" do
    h = new_hash 5
    h.clear
    h.default.should == 5

    h = new_hash("a" => 100, "b" => 200)
    h.default = "Go fish"
    h.clear
    h["z"].should == "Go fish"
  end

  it "does not remove default procs" do
    h = new_hash { 5 }
    h.clear
    h.default_proc.should_not == nil
  end

  ruby_version_is ""..."1.9" do
    it "raises a TypeError if called on a frozen instance" do
      lambda { HashSpecs.frozen_hash.clear  }.should raise_error(TypeError)
      lambda { HashSpecs.empty_frozen_hash.clear }.should raise_error(TypeError)
    end
  end
  ruby_version_is "1.9" do
    it "raises a RuntimeError if called on a frozen instance" do
      lambda { HashSpecs.frozen_hash.clear  }.should raise_error(RuntimeError)
      lambda { HashSpecs.empty_frozen_hash.clear }.should raise_error(RuntimeError)
    end
  end
end
