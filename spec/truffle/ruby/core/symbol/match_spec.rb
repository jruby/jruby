require File.expand_path('../../../spec_helper', __FILE__)

describe :symbol_match, :shared => true do
  it "returns the index of the beginning of the match" do
    :abc.send(@method, /b/).should == 1
  end

  it "returns nil if there is no match" do
    :a.send(@method, /b/).should be_nil
  end

  it "sets the last match pseudo-variables" do
    :a.send(@method, /(.)/).should == 0
    $1.should == "a"
  end
end

ruby_version_is "1.9" do
  describe "Symbol#=~" do
    it_behaves_like :symbol_match, :=~
  end

  describe "Symbol#match" do
    it_behaves_like :symbol_match, :match
  end
end
