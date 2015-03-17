require File.expand_path('../../../spec_helper', __FILE__)

describe "IOError" do
  it "is a superclass of EOFError" do
    IOError.should be_ancestor_of(EOFError)
  end
end
