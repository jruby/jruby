require File.expand_path('../../../../spec_helper', __FILE__)
require 'matrix'

ruby_version_is "1.9.3" do
  describe "Matrix::LUPDecomposition#determinant" do
    it "returns the determinant when the matrix is square" do
      a = Matrix[[7, 8, 9], [14, 46, 51], [28, 82, 163]]
      a.lup.determinant.should == 15120 # == a.determinant
    end

    ruby_bug "#7620", "1.9.3" do
      it "raises an error for rectangular matrices" do
        [
          Matrix[[7, 8, 9], [14, 46, 51]],
          Matrix[[7, 8], [14, 46], [28, 82]],
        ].each do |m|
          lup = m.lup
          lambda {
            lup.determinant
          }.should raise_error(Matrix::ErrDimensionMismatch)
        end
      end
    end
  end
end
