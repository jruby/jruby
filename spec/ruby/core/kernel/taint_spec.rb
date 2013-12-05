require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Kernel#taint" do
  it "returns self" do
    o = Object.new
    o.taint.should equal(o)
  end

  it "sets the tainted bit" do
    o = Object.new
    o.taint
    o.tainted?.should == true
  end

  ruby_version_is ""..."1.9" do
    it "raises TypeError on an untainted, frozen object" do
      o = Object.new.freeze
      lambda { o.taint }.should raise_error(TypeError)
    end
  end

  ruby_version_is "1.9" do
    it "raises RuntimeError on an untainted, frozen object" do
      o = Object.new.freeze
      lambda { o.taint }.should raise_error(RuntimeError)
    end
  end

  it "does not raise an error on a tainted, frozen object" do
    o = Object.new.taint.freeze
    o.taint.should equal(o)
  end

  it "has no effect on immediate values" do
    [nil, true, false, :sym].each do |v|
      v.taint
      v.tainted?.should == false
    end
  end

  ruby_version_is ""..."2.0" do
    it "has no effect on immediate values" do
      [1].each do |v|
        v.taint
        v.tainted?.should == false
      end
    end
  end

  ruby_version_is "2.0" do
  it "raises error on fixnum values" do
    [1].each do |v|
      lambda { v.taint }.should raise_error(RuntimeError)
    end
  end
  end
end
