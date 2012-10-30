require File.expand_path('../../../spec_helper', __FILE__)

describe "Object.new" do
  it "creates a new Object" do
    Object.new.should be_kind_of(Object)
  end

  ruby_version_is "1.9.1".."1.9.2" do # Ref: [redmine:2451]
    it "accepts any number of arguments" do
      lambda {
        Object.new("This", "makes it easier", "to call super", "from other constructors")
      }.should_not raise_error
    end
  end

  ruby_version_is "1.9.3" do # Ref: [redmine:2451]
    it "doesn't accept arguments" do
      lambda {
        Object.new("This", "makes it easier", "to call super", "from other constructors")
      }.should raise_error
    end
  end
end

