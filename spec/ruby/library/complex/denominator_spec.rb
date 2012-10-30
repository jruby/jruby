require File.expand_path('../../../shared/complex/denominator', __FILE__)

ruby_version_is ""..."1.9" do

  require 'complex'

  # FIXME:
  # Complex#denominator requires the rational library,
  # as Integer#denominator is defined by it.
  # I think this method is pretty buggy, as there is no
  # denominator for Floats and rational might not always
  # be loaded, both resulting in a method missing exception.
  # Also, the documentation for Complex#denominator does
  # not mention a dependency for rational.
  require "rational"

  describe "Complex#denominator" do
    it_behaves_like(:complex_denominator, :denominator)
  end
end
