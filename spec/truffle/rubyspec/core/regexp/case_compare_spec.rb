require File.expand_path('../../../spec_helper', __FILE__)

describe "Regexp#===" do
  it "is true if there is a match" do
    (/abc/ === "aabcc").should be_true
  end

  it "is false if there is no match" do
    (/abc/ === "xyz").should be_false
  end

  ruby_version_is "1.9" do
    it "returns true if it matches a Symbol" do
      (/a/ === :a).should be_true
    end

    it "returns false if it does not match a Symbol" do
      (/a/ === :b).should be_false
    end
  end
end
