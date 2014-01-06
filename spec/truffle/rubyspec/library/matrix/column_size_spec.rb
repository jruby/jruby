require File.expand_path('../../../spec_helper', __FILE__)
require 'matrix'

describe "Matrix#column_size" do
  it "returns the number of columns" do
    Matrix[ [1,2], [3,4] ].column_size.should == 2
  end

  ruby_bug "redmine:1532", "1.8.7" do
    it "returns 0 for empty matrices" do
      Matrix[ [], [] ].column_size.should == 0
      Matrix[ ].column_size.should == 0
    end
  end
end
