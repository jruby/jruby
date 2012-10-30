require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is ""..."1.9" do
  describe "Regexp#kcode" do
    it "returns the character set code" do
      default = /f.(o)/.kcode
      default.should_not == 'sjis'
      default.should_not == 'euc'
      default.should_not == 'utf8'

      /ab+c/s.kcode.should == "sjis"
      /a(.)+s/n.kcode.should == "none"
      /xyz/e.kcode.should == "euc"
      /cars/u.kcode.should == "utf8"
    end
  end
end
