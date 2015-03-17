require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "".."1.9" do
  require 'thread'

  describe "Thread.exclusive" do
    before :each do
      ScratchPad.clear
    end

    it "sets Thread.critical to true and yields" do
      Thread.exclusive { ScratchPad.record Thread.critical }
      ScratchPad.recorded.should == true
    end

    it "returns the result of yielding" do
      Thread.exclusive { :result }.should == :result
    end

    it "resets Thread.critical after yielding" do
      Thread.exclusive {}
      Thread.critical.should be_false
    end

    it "resets Thread.critical if the block raises" do
      lambda { Thread.exclusive { raise Exception } }.should raise_error(Exception)
      Thread.critical.should be_false
    end
  end
end
