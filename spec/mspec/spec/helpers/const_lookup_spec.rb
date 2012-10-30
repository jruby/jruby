require 'spec_helper'
require 'mspec/guards'
require 'mspec/helpers'

CONST = 2

module ConstLookupSpecs
  class A
    class B
      CONST = 1
    end

    class C; end

    class D
      def self.const_missing(const)
        A::B::CONST
      end
    end
  end
end

describe Kernel, "#const_lookup" do
  it "returns the constant specified by 'A::B'" do
    const_lookup("ConstLookupSpecs::A::B").should == ConstLookupSpecs::A::B
  end

  it "returns a regular constant specified without scoping" do
    const_lookup("ConstLookupSpecs").should == ConstLookupSpecs
  end

  it "returns an explicit toplevel constant" do
    const_lookup("::ConstLookupSpecs").should == ConstLookupSpecs
  end

  it "returns the constant from the proper scope" do
    const_lookup("ConstLookupSpecs::A::B::CONST").should == 1
  end

  it "raises NameError if the constant is not contained within the module's scope" do
    lambda {
      const_lookup("ConstLookupSpecs::A::C::CONST")
    }.should raise_error(NameError)
  end

  it "returns the value of #const_missing" do
    const_lookup("ConstLookupSpecs::A::D::CONST").should == 1
  end
end
