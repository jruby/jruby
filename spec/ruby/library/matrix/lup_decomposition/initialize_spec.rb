require File.expand_path('../../../../spec_helper', __FILE__)
require 'matrix'

ruby_version_is "1.9.3" do
  describe "Matrix::LUPDecomposition#initialize" do
    it "raises an error if argument is not a matrix" do
      lambda {
        Matrix::LUPDecomposition.new([[]])
      }.should raise_error(TypeError)
      lambda {
        Matrix::LUPDecomposition.new(42)
      }.should raise_error(TypeError)
    end
  end
end
