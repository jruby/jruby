
describe :string_slice_regexp_group, :shared => true do

  it "returns the capture for the given name" do
    "hello there".send(@method, /(?<g>[aeiou](.))/, 'g').should == "el"
    "hello there".send(@method, /[aeiou](?<g>.)/, 'g').should == "l"

    "har".send(@method, /(?<g>(.)(.)(.))/, 'g').should == "har"
    "har".send(@method, /(?<h>.)(.)(.)/, 'h').should == "h"
    "har".send(@method, /(.)(?<a>.)(.)/, 'a').should == "a"
    "har".send(@method, /(.)(.)(?<r>.)/, 'r').should == "r"
    "har".send(@method, /(?<h>.)(?<a>.)(?<r>.)/, 'r').should == "r"
  end

  it "returns the last capture for duplicate names" do
    "hello there".send(@method, /(?<g>h)(?<g>.)/, 'g').should == "e"
    "hello there".send(@method, /(?<g>h)(?<g>.)(?<f>.)/, 'g').should == "e"
  end

  it "returns the innermost capture for nested duplicate names" do
    "hello there".send(@method, /(?<g>h(?<g>.))/, 'g').should == "e"
  end

  it "always taints resulting strings when self or regexp is tainted" do
    strs = ["hello world"]
    strs += strs.map { |s| s.dup.taint }

    strs.each do |str|
      str.send(@method, /(?<hi>hello)/, 'hi').tainted?.should == str.tainted?

      str.send(@method, /(?<g>(.)(.)(.))/, 'g').tainted?.should == str.tainted?
      str.send(@method, /(?<h>.)(.)(.)/, 'h').tainted?.should == str.tainted?
      str.send(@method, /(.)(?<a>.)(.)/, 'a').tainted?.should == str.tainted?
      str.send(@method, /(.)(.)(?<r>.)/, 'r').tainted?.should == str.tainted?
      str.send(@method, /(?<h>.)(?<a>.)(?<r>.)/, 'r').tainted?.should == str.tainted?

      tainted_re = /(?<a>.)(?<b>.)(?<c>.)/
      tainted_re.taint

      str.send(@method, tainted_re, 'a').tainted?.should be_true
      str.send(@method, tainted_re, 'b').tainted?.should be_true
      str.send(@method, tainted_re, 'c').tainted?.should be_true
    end
  end

  it "returns nil if there is no match" do
    "hello there".send(@method, /(?<whut>what?)/, 'whut').should be_nil
  end

  it "raises an IndexError if there is no capture for the given name" do
    lambda do
      "hello there".send(@method, /[aeiou](.)\1/, 'non')
    end.should raise_error(IndexError)
  end

  it "raises a TypeError when the given name is not a String" do
    lambda { "hello".send(@method, /(?<q>.)/, mock('x')) }.should raise_error(TypeError)
    lambda { "hello".send(@method, /(?<q>.)/, {})        }.should raise_error(TypeError)
    lambda { "hello".send(@method, /(?<q>.)/, [])        }.should raise_error(TypeError)
  end

  it "raises an IndexError when given the empty String as a group name" do
    lambda { "hello".send(@method, /(?<q>)/, '') }.should raise_error(IndexError)
  end

  it "returns subclass instances" do
    s = StringSpecs::MyString.new("hello")
    s.send(@method, /(?<q>.)/, 'q').should be_kind_of(StringSpecs::MyString)
  end

  it "sets $~ to MatchData when there is a match and nil when there's none" do
    'hello'.send(@method, /(?<hi>.(.))/, 'hi')
    $~[0].should == 'he'

    'hello'.send(@method, /(?<non>not)/, 'non')
    $~.should be_nil
  end
end
