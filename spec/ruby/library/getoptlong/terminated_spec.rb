require File.expand_path('../../../spec_helper', __FILE__)
require 'getoptlong'

describe "GetoptLong#terminated?" do
  it "returns true if option processing has terminated" do
    begin
      old_argv_value = ARGV
      ARGV = [ "--size", "10k" ]
      opts = GetoptLong.new(["--size", GetoptLong::REQUIRED_ARGUMENT])
      opts.terminated?.should == false

      opts.get.should == ["--size", "10k"]
      opts.terminated?.should == false

      opts.get.should == nil
      opts.terminated?.should == true
    ensure
      ARGV = old_argv_value
    end
  end
end
