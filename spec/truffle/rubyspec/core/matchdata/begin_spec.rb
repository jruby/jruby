# -*- encoding: utf-8 -*-

require File.expand_path('../../../spec_helper', __FILE__)

describe "MatchData#begin" do
  it "returns the offset of the start of the nth element" do
    match_data = /(.)(.)(\d+)(\d)/.match("THX1138.")
    match_data.begin(0).should == 1
    match_data.begin(2).should == 2
  end

  ruby_version_is ""..."1.9" do
    it "returns the offset for multi byte strings" do
      match_data = /(.)(.)(\d+)(\d)/.match("TñX1138.")
      match_data.begin(0).should == 2
      match_data.begin(2).should == 3
    end

    it "returns the offset for multi byte strings with unicode regexp" do
      match_data = /(.)(.)(\d+)(\d)/u.match("TñX1138.")
      match_data.begin(0).should == 1
      match_data.begin(2).should == 3
    end
  end

  ruby_version_is "1.9" do
    it "returns the offset for multi byte strings" do
      match_data = /(.)(.)(\d+)(\d)/.match("TñX1138.")
      match_data.begin(0).should == 1
      match_data.begin(2).should == 2
    end

    it "returns the offset for multi byte strings with unicode regexp" do
      match_data = /(.)(.)(\d+)(\d)/u.match("TñX1138.")
      match_data.begin(0).should == 1
      match_data.begin(2).should == 2
    end
  end

end
