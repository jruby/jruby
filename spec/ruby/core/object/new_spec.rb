require File.expand_path('../../../spec_helper', __FILE__)

describe "Object.new" do
  it "creates a new Object" do
    Object.new.should be_kind_of(Object)
  end

  it "doesn't accept arguments" do
    lambda {
      Object.new("This", "makes it easier", "to call super", "from other constructors")
    }.should raise_error
  end
end
