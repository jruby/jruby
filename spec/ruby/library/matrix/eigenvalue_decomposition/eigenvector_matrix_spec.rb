require File.expand_path('../../../../spec_helper', __FILE__)
require 'matrix'

ruby_version_is "1.9.3" do
  describe "Matrix::EigenvalueDecomposition#eigenvector_matrix" do
    ruby_bug '#7208', '1.9.3' do
      it "returns a complex eigenvector matrix given a rotation matrix" do
        # Fix me: should test for linearity, not for equality
        Matrix[[ 1, 1],
               [-1, 1]].eigensystem.eigenvector_matrix.should ==
        Matrix[[1, 1],
               [Complex(0, 1), Complex(0, -1)]]
      end
    end

    it "returns an real eigenvector matrix for a symetric matrix" do
      # Fix me: should test for linearity, not for equality
      Matrix[[1, 2],
             [2, 1]].eigensystem.eigenvector_matrix.should ==
      Matrix[[0.7071067811865475, 0.7071067811865475],
             [-0.7071067811865475, 0.7071067811865475]]
    end
  end
end
