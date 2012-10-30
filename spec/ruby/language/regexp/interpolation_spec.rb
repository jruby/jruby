require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/classes', __FILE__)

describe "Regexps with interpolation" do

  it "allow interpolation of strings" do
    str = "foo|bar"
    /#{str}/.should == /foo|bar/
  end

  it "allows interpolation of literal regexps" do
    re = /foo|bar/
    /#{re}/.should == /(?-mix:foo|bar)/
  end

  it "allows interpolation of any class that responds to to_s" do
    o = LanguageSpecs::ClassWith_to_s.new
    /#{o}/.should == /class_with_to_s/
  end

  it "allows interpolation which mixes modifiers" do
    re = /foo/i
    /#{re} bar/m.should == /(?i-mx:foo) bar/m
  end

  it "allows interpolation to interact with other Regexp constructs" do
    str = "foo)|(bar"
    /(#{str})/.should == /(foo)|(bar)/

    str = "a"
    /[#{str}-z]/.should == /[a-z]/
  end

  it "gives precedence to escape sequences over substitution" do
    str = "J"
    /\c#{str}/.to_s.should == '(?-mix:\c#' + '{str})'
  end

  it "throws RegexpError for malformed interpolation" do
    s = ""
    lambda { /(#{s}/ }.should raise_error(RegexpError)
    s = "("
    lambda { /#{s}/ }.should raise_error(RegexpError)
  end

  it "allows interpolation in extended mode" do
    var = "#comment\n  foo  #comment\n  |  bar"
    (/#{var}/x =~ "foo").should == (/foo|bar/ =~ "foo")
  end

  it "allows escape sequences in interpolated regexps" do
    escape_seq = %r{"\x80"}n
    %r{#{escape_seq}}n.should == /(?-mix:"\x80")/n
  end
end
