# -*- encoding: utf-8 -*-
require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "Symbol#downcase" do
    it "returns a Symbol" do
      :glark.downcase.should be_an_instance_of(Symbol)
    end

    it "converts uppercase ASCII characters to their lowercase equivalents" do
      :lOwEr.downcase.should == :lower
    end

    it "leaves lowercase Unicode characters as they were" do
      "\u{C0}Bc".to_sym.downcase.should == :"Àbc"
    end

    it "leaves uppercase Unicode characters as they were" do
      "\u{DE}Bc".to_sym.downcase.should == :"Þbc"
    end

    it "leaves non-alphabetic ASCII characters as they were" do
      "Glark?!?".to_sym.downcase.should == :"glark?!?"
    end
  end
end
