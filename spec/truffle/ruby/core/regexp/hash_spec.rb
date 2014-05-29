require File.expand_path('../../../spec_helper', __FILE__)

describe "Regexp#hash" do
  it "is provided" do
    Regexp.new('').respond_to?(:hash).should == true
  end

  it "is based on the text and options of Regexp" do
    (/cat/ix.hash == /cat/ixn.hash).should == true
    (/dog/m.hash  == /dog/m.hash).should == true
    (/cat/.hash   == /cat/ix.hash).should == false
    (/cat/.hash   == /dog/.hash).should == false
  end

  it "returns the same value for two Regexps differing only in the /n option" do
    (//.hash == //n.hash).should == true
  end
end
