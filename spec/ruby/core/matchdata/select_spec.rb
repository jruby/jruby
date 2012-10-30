require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is ""..."1.9" do
  describe "MatchData#select" do
    it "yields the contents of the match array to a block" do
       /(.)(.)(\d+)(\d)/.match("THX1138: The Movie").select { |x| x }.should == ["HX1138", "H", "X", "113", "8"]
    end
  end
end
