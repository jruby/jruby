require File.expand_path('../../../spec_helper', __FILE__)
require 'matrix'

describe "Matrix#rank" do
  it "returns the rank of the Matrix" do
    Matrix[ [7,6], [3,9] ].rank.should == 2
  end

  # The examples below seems to indicate a regression in MRI 1.9.1, as it works
  # in 1.8.7. On 1.9.1 it just hangs.
  ruby_bug "#1020", "1.9.1.500" do
    it "doesn't loop forever" do
      Matrix[ [1,2,3], [4,5,6], [7,8,9] ].rank.should == 2
      Matrix[ [1, 2, 0, 3], [1, -2, 3, 0], [0, 0, 4, 8], [2, 4, 0, 6] ].rank.
      should == 3
    end
  end

  ruby_bug "revision 24969", "1.8.7" do
    it "works for some easy rectangular matrices" do
      Matrix[[0,0],[0,0],[1,0]].rank.should == 1
      Matrix[[0,1],[0,0],[1,0]].rank.should == 2
    end
  end
end
