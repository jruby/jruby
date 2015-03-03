require File.expand_path('../../../../../../ruby/spec_helper', __FILE__)

describe "Regexp#match_start" do
  it "matches a string N bytes from the beginning" do
    m = /b/.match_start("ab", 1)
    m[0].should == "b"
  end

  it "preserves zero length captures" do
    r = /(a)?/
    str = "ab"

    m = r.match_start(str, 0)
    m[0].should == "a"
    m[1].should == "a"

    m = r.match_start(str, 1)
    m[0].should == ""
    m[1].should be_nil
  end
end
