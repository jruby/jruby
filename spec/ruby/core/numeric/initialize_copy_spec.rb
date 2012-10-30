require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Numeric#singleton_method_added" do
  it "raises a TypeError when trying to #dup a Numeric" do
    lambda do
      a = NumericSpecs::Subclass.new
      a.dup
    end.should raise_error(TypeError)

    lambda do
      a = 1
      a.dup
    end.should raise_error(TypeError)

    lambda do
      a = 1.5
      a.dup
    end.should raise_error(TypeError)

    lambda do
      a = bignum_value
      a.dup
    end.should raise_error(TypeError)
  end
end
