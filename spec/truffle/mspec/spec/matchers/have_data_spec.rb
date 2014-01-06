require 'spec_helper'
require 'mspec/expectations/expectations'
require 'mspec/guards'
require 'mspec/helpers'
require 'mspec/matchers'

describe HaveDataMatcher do
  before :each do
    @name = tmp "have_data_matcher"
    touch(@name) { |f| f.puts "123abc" }
  end

  after :each do
    rm_r @name
  end

  it "raises an IOError if the named file does not exist" do
    lambda do
      HaveDataMatcher.new("123").matches?("no_file.txt")
    end.should raise_error(Errno::ENOENT)
  end

  it "matches when the named file begins with the same bytes as data" do
    HaveDataMatcher.new("123a").matches?(@name).should be_true
  end

  it "does not match when the named file begins with fewer bytes than data" do
    HaveDataMatcher.new("123abcPQR").matches?(@name).should be_false
  end

  it "does not match when the named file begins with different bytes than data" do
    HaveDataMatcher.new("abc1").matches?(@name).should be_false
  end

  it "accepts an optional mode argument to open the data file" do
    HaveDataMatcher.new("123a", "r").matches?(@name).should be_true
  end

  it "provides a useful failure message" do
    matcher = HaveDataMatcher.new("abc1")
    matcher.matches?(@name)
    matcher.failure_message.should == [
      "Expected #{@name}", "to have data \"abc1\"\n"
    ]
  end

  it "provides a useful negative failure message" do
    matcher = HaveDataMatcher.new("123abc")
    matcher.matches?(@name)
    matcher.negative_failure_message.should == [
      "Expected #{@name}", "not to have data \"123abc\"\n"
    ]
  end
end
