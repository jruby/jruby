# -*- encoding: utf-8 -*-
require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "Symbol#upcase" do
    it "returns a Symbol" do
      :glark.upcase.should be_an_instance_of(Symbol)
    end

    it "converts lowercase ASCII characters to their uppercase equivalents" do
      :lOwEr.upcase.should == :LOWER
    end

    it "leaves lowercase Unicode characters as they were" do
      "\u{C0}Bc".to_sym.upcase.should == :"ÀBC"
    end

    it "leaves non-alphabetic ASCII characters as they were" do
      "Glark?!?".to_sym.upcase.should == :"GLARK?!?"
    end
  end
end
