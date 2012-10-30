# -*- encoding: utf-8 -*-

require File.expand_path('../../../spec_helper', __FILE__)

describe "MatchData#offset" do
  it "returns a two element array with the begin and end of the nth match" do
    match_data = /(.)(.)(\d+)(\d)/.match("THX1138.")
    match_data.offset(0).should == [1, 7]
    match_data.offset(4).should == [6, 7]
  end

  ruby_version_is ""..."1.9" do
    it "returns the offset for multi byte strings" do
      match_data = /(.)(.)(\d+)(\d)/.match("TñX1138.")
      match_data.offset(0).should == [2, 8]
      match_data.offset(4).should == [7, 8]
    end

    it "returns the offset for multi byte strings with unicode regexp" do
      match_data = /(.)(.)(\d+)(\d)/u.match("TñX1138.")
      match_data.offset(0).should == [1, 8]
      match_data.offset(4).should == [7, 8]
    end
  end

  ruby_version_is "1.9" do
    it "returns the offset for multi byte strings" do
      match_data = /(.)(.)(\d+)(\d)/.match("TñX1138.")
      match_data.offset(0).should == [1, 7]
      match_data.offset(4).should == [6, 7]
    end

    it "returns the offset for multi byte strings with unicode regexp" do
      match_data = /(.)(.)(\d+)(\d)/u.match("TñX1138.")
      match_data.offset(0).should == [1, 7]
      match_data.offset(4).should == [6, 7]
    end
  end

end
