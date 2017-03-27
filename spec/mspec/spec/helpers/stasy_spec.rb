require 'spec_helper'
require 'mspec/guards'
require 'mspec/helpers'

describe Object, "#stasy" do
  before :each do
    MSpec.stub :deprecate
  end

  it "returns a Symbol when passed a String" do
    stasy("nom").should == :nom
  end

  it "returns a Symbol when passed a Symbol" do
    stasy(:some).should == :some
  end

  it "returns an Array of Symbols when passed an Array of Strings" do
    stasy("some", "thing").should == [:some, :thing]
  end

  it "returns an Array of Symbols when passed an Array of Symbols" do
    stasy(:nom, :nom).should == [:nom, :nom]
  end
end
