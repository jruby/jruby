require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes.rb', __FILE__)

describe "String#each_byte" do
  it "passes each byte in self to the given block" do
    a = []
    "hello\x00".each_byte { |c| a << c }
    a.should == [104, 101, 108, 108, 111, 0]
  end

  it "keeps iterating from the old position (to new string end) when self changes" do
    r = ""
    s = "hello world"
    s.each_byte do |c|
      r << c
      s.insert(0, "<>") if r.size < 3
    end
    r.should == "h><>hello world"

    r = ""
    s = "hello world"
    s.each_byte { |c| s.slice!(-1); r << c }
    r.should == "hello "

    r = ""
    s = "hello world"
    s.each_byte { |c| s.slice!(0); r << c }
    r.should == "hlowrd"

    r = ""
    s = "hello world"
    s.each_byte { |c| s.slice!(0..-1); r << c }
    r.should == "h"
  end

  it "returns self" do
    s = "hello"
    (s.each_byte {}).should equal(s)
  end

  ruby_version_is "1.8.7" do
    it "returns an enumerator when no block given" do
      enum = "hello".each_byte
      enum.should be_an_instance_of(enumerator_class)
      enum.to_a.should == [104, 101, 108, 108, 111]
    end
  end

end
