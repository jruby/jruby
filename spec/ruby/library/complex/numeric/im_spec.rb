require File.expand_path('../../../../spec_helper', __FILE__)

require 'complex'

ruby_version_is ''...'2.2' do
  describe "Numeric#im" do
    it "returns a new Complex number with self as the imaginary component" do
      20.im.should == Complex(0, 20)
      (-4.5).im.should == Complex(0, -4.5)
      bignum_value.im.should == Complex(0, bignum_value)
    end
  end
end
