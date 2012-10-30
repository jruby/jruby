require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is ""..."1.9" do

  require 'complex'

  describe "Complex.new" do
    it "returns a new Complex number" do
      Complex.new(1, 2).should be_kind_of(Complex)
    end

    it "raises a TypeError when one of the given arguments is not Numeric" do
      lambda { Complex.new(1, "foo") }.should raise_error(TypeError)
      lambda { Complex.new("bar", 2) }.should raise_error(TypeError)
    end

    it "raises a TypeError when one of the given arguments is a Complex" do
      lambda { Complex.new(Complex(1), "foo") }.should raise_error(TypeError)
      lambda { Complex.new("bar", Complex(3, 5)) }.should raise_error(TypeError)
    end
  end

  describe "Complex.new!" do
    it "returns a new Complex number" do
      Complex.new(1, 2).should be_kind_of(Complex)
    end

    it "defaults to 0 for the imaginery part" do
      a = Complex.new!(3)
      a.image.should == 0
    end

    it "raises a TypeError when one of the given arguments is not Numeric" do
      lambda { Complex.new(1, "foo") }.should raise_error(TypeError)
      lambda { Complex.new("bar", 2) }.should raise_error(TypeError)
    end

    it "raises a TypeError when one of the given arguments is a Complex" do
      lambda { Complex.new(Complex(1), "foo") }.should raise_error(TypeError)
      lambda { Complex.new("bar", Complex(3, 5)) }.should raise_error(TypeError)
    end
  end
end
