require File.expand_path('../../../spec_helper', __FILE__)

describe "MatchData#pre_match" do
  it "returns the string before the match, equiv. special var $`" do
    /(.)(.)(\d+)(\d)/.match("THX1138: The Movie").pre_match.should == 'T'
    $`.should == 'T'
  end

  with_feature :encoding do
    it "sets the encoding to the encoding of the source String" do
      str = "abc".force_encoding Encoding::EUC_JP
      str.match(/b/).pre_match.encoding.should equal(Encoding::EUC_JP)
    end

    it "sets an empty result to the encoding of the source String" do
      str = "abc".force_encoding Encoding::ISO_8859_1
      str.match(/a/).pre_match.encoding.should equal(Encoding::ISO_8859_1)
    end
  end
end
