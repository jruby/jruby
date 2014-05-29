# -*- encoding: utf-8 -*-
require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes.rb', __FILE__)

language_version __FILE__, "match"

describe "String#=~" do
  it "behaves the same way as index() when given a regexp" do
    ("rudder" =~ /udder/).should == "rudder".index(/udder/)
    ("boat" =~ /[^fl]oat/).should == "boat".index(/[^fl]oat/)
    ("bean" =~ /bag/).should == "bean".index(/bag/)
    ("true" =~ /false/).should == "true".index(/false/)
  end

  it "raises a TypeError if a obj is a string" do
    lambda { "some string" =~ "another string" }.should raise_error(TypeError)
    lambda { "a" =~ StringSpecs::MyString.new("b")          }.should raise_error(TypeError)
  end

  it "invokes obj.=~ with self if obj is neither a string nor regexp" do
    str = "w00t"
    obj = mock('x')

    obj.should_receive(:=~).with(str).any_number_of_times.and_return(true)
    str.should =~ obj

    obj = mock('y')
    obj.should_receive(:=~).with(str).any_number_of_times.and_return(false)
    str.should_not =~ obj
  end

  it "sets $~ to MatchData when there is a match and nil when there's none" do
    'hello' =~ /./
    $~[0].should == 'h'

    'hello' =~ /not/
    $~.should == nil
  end
end

describe "String#match" do
  it "matches the pattern against self" do
    'hello'.match(/(.)\1/)[0].should == 'll'
  end

  ruby_version_is "1.9" do
    it_behaves_like :string_match_escaped_literal, :match

    it "matches the pattern against self starting at an optional index" do
      "hello".match(/(.+)/,2)[0].should == 'llo'
    end

    it "uses the start as a character offset" do
      "hüllo".match(/(.+)/, 2)[0].should == 'llo'
    end

    describe "when passed a block" do
      it "yields the MatchData" do
        "abc".match(/./) {|m| ScratchPad.record m }
        ScratchPad.recorded.should be_kind_of(MatchData)
      end

      it "returns the block result" do
        "abc".match(/./) { :result }.should == :result
      end

      it "does not yield if there is no match" do
        ScratchPad.record []
        "b".match(/a/) {|m| ScratchPad << m }
        ScratchPad.recorded.should == []
      end
    end
  end

  it "tries to convert pattern to a string via to_str" do
    obj = mock('.')
    def obj.to_str() "." end
    "hello".match(obj)[0].should == "h"

    obj = mock('.')
    def obj.respond_to?(type, *) true end
    def obj.method_missing(*args) "." end
    "hello".match(obj)[0].should == "h"
  end

  it "raises a TypeError if pattern is not a regexp or a string" do
    lambda { 'hello'.match(10)   }.should raise_error(TypeError)
    lambda { 'hello'.match(:ell) }.should raise_error(TypeError)
  end

  it "converts string patterns to regexps without escaping" do
    'hello'.match('(.)\1')[0].should == 'll'
  end

  it "returns nil if there's no match" do
    'hello'.match('xx').should == nil
  end

  it "matches \\G at the start of the string" do
    'hello'.match(/\Gh/)[0].should == 'h'
    'hello'.match(/\Go/).should == nil
  end

  it "sets $~ to MatchData of match or nil when there is none" do
    'hello'.match(/./)
    $~[0].should == 'h'
    Regexp.last_match[0].should == 'h'

    'hello'.match(/X/)
    $~.should == nil
    Regexp.last_match.should == nil
  end
end
