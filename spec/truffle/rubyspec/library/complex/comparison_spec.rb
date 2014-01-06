require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is ""..."1.9" do

  require 'complex'

  describe "Complex#<=>" do
    it "compares the absolute values of self and other" do
      (Complex(1, 2) <=> Complex(2, 1)).should == 0
      (Complex(-3, -10) <=> Complex(2, 1)).should > 0
      (Complex(3, 5) <=> Complex(100.0, -190.5)).should < 0

      (Complex(3, 4) <=> 5).should == 0
      (Complex(3, 4) <=> -5).should == 0
      (Complex(-3, -4) <=> -5).should == 0

      (Complex(3, 4) <=> 6).should < 0
      (Complex(3, 4) <=> -4).should > 0

      (Complex(3, 4) <=> 6.0).should < 0
      (Complex(3, 4) <=> -4.0).should > 0
    end
  end
end
