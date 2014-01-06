ruby_version_is "1.9" do
  describe "MatchData#[Symbol]" do
    it "returns the corresponding named match when given a Symbol" do
      md = 'haystack'.match(/(?<t>t(?<a>ack))/)
      md[:a].should == 'ack'
      md[:t].should == 'tack'
    end

    it "returns the corresponding named match when given a String" do
      md = 'haystack'.match(/(?<t>t(?<a>ack))/)
      md['a'].should == 'ack'
      md['t'].should == 'tack'
    end

    it "raises an IndexError if there is no named match corresponding to the Symbol" do
      md = 'haystack'.match(/(?<t>t(?<a>ack))/)
      lambda do
        md[:hay]
      end.should raise_error(IndexError)
    end

    it "raises an IndexError if there is no named match corresponding to the String" do
      md = 'haystack'.match(/(?<t>t(?<a>ack))/)
      lambda do
        md['hay']
      end.should raise_error(IndexError)
    end

    it "returns matches in the String's encoding" do
      rex = /(?<t>t(?<a>ack))/u
      md = 'haystack'.force_encoding('euc-jp').match(rex)
      md[:t].encoding.should == Encoding::EUC_JP
    end
  end
end
