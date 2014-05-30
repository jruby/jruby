require 'matrix'

describe :determinant, :shared => true do
  # Ruby versions less than 1.9.0, AFAICT, return the wrong determinant in
  # most non-trivial cases. As the rdoc for Matrix suggests, "require 'mathn'"
  # seems to fix this, but as Matrix doesn't require that library itself, the
  # bug remains. I've reported http://redmine.ruby-lang.org/issues/show/1516 ,
  # which is ostensibly about the documentation for #determinant not
  # reflecting this bug, but raises the question of why 1.8.7 doesn't handle
  # this properly.
  ruby_bug "#1516", "1.8.7" do
    it "returns the determinant of a square Matrix" do
      m = Matrix[ [7,6], [3,9] ]
      m.send(@method).should == 45

      m = Matrix[ [9, 8], [6,5] ]
      m.send(@method).should == -3

      m = Matrix[ [9,8,3], [4,20,5], [1,1,1] ]
      m.send(@method).should == 95
    end
  end

  it "returns the determinant of a single-element Matrix" do
    m = Matrix[ [2] ]
    m.send(@method).should == 2
  end

  ruby_bug "redmine:1532", "1.8.7" do
    it "returns 1 for an empty Matrix" do
      m = Matrix[ ]
      m.send(@method).should == 1
    end
  end

  ruby_bug "#1531", "1.8.7" do
    it "returns the determinant even for Matrices containing 0 as first entry" do
      Matrix[[0,1],[1,0]].send(@method).should == -1
    end
  end

  ruby_bug "#2770", "1.8.7" do
    it "raises an error for rectangular matrices" do
      lambda {
        Matrix[[1], [2], [3]].send(@method)
      }.should raise_error(Matrix::ErrDimensionMismatch)

      lambda {
        Matrix.empty(3,0).send(@method)
      }.should raise_error(Matrix::ErrDimensionMismatch)
    end
  end
end
