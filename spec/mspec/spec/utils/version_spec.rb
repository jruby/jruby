require 'spec_helper'
require 'mspec/utils/version'

describe SpecVersion, "#to_s" do
  it "returns the string with which it was initialized" do
    SpecVersion.new("1.8").to_s.should == "1.8"
    SpecVersion.new("2.118.9.2").to_s.should == "2.118.9.2"
  end
end

describe SpecVersion, "#to_str" do
  it "returns the same string as #to_s" do
    version = SpecVersion.new("2.118.9.2")
    version.to_str.should == version.to_s
  end
end

describe SpecVersion, "#to_i with ceil = false" do
  it "returns an integer representation of the version string" do
    SpecVersion.new("1.8.6.22").to_i.should == 10108060022
  end

  it "replaces missing version parts with zeros" do
    SpecVersion.new("1.8").to_i.should == 10108000000
    SpecVersion.new("1.8.6").to_i.should == 10108060000
    SpecVersion.new("1.8.7.333").to_i.should == 10108070333
  end
end

describe SpecVersion, "#to_i with ceil = true" do
  it "returns an integer representation of the version string" do
    SpecVersion.new("1.8.6.22", true).to_i.should == 10108060022
  end

  it "fills in 9s for missing tiny and patchlevel values" do
    SpecVersion.new("1.8", true).to_i.should == 10108999999
    SpecVersion.new("1.8.6", true).to_i.should == 10108069999
    SpecVersion.new("1.8.7.333", true).to_i.should == 10108070333
  end
end

describe SpecVersion, "#to_int" do
  it "returns the same value as #to_i" do
    version = SpecVersion.new("4.16.87.333")
    version.to_int.should == version.to_i
  end
end
