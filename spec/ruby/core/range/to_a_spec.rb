require_relative '../../spec_helper'

describe "Range#to_a" do
  it "converts self to an array" do
    (-5..5).to_a.should == [-5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5]
    ('A'..'D').to_a.should == ['A','B','C','D']
    ('A'...'D').to_a.should == ['A','B','C']
    (0xfffd...0xffff).to_a.should == [0xfffd,0xfffe]
    -> { (0.5..2.4).to_a }.should.raise(TypeError)
  end

  it "returns empty array for descending-ordered" do
    (5..-5).to_a.should == []
    ('D'..'A').to_a.should == []
    ('D'...'A').to_a.should == []
    (0xffff...0xfffd).to_a.should == []
  end

  it "works with Ranges of 64-bit integers" do
    large = 1 << 40
    (large..large+1).to_a.should == [1099511627776, 1099511627777]
  end

  it "works with inclusive and exclusive Integer ranges" do
    (1..1).to_a.should == [1]
    (1...1).to_a.should == []
    (1...2).to_a.should == [1]
    (-2..2).to_a.should == [-2, -1, 0, 1, 2]
    (-2...2).to_a.should == [-2, -1, 0, 1]
    (-5..-3).to_a.should == [-5, -4, -3]
  end

  it "returns an empty array for descending-ordered Integer ranges" do
    (1...0).to_a.should == []
    (-3..-5).to_a.should == []
    (fixnum_max..fixnum_min).to_a.should == []
    (fixnum_max...fixnum_min).to_a.should == []
  end

  it "works with Integer ranges larger than the small Integer cache" do
    (-500...500).to_a.should == Array.new(1000) { |i| i - 500 }
    (250..260).to_a.should == [250, 251, 252, 253, 254, 255, 256, 257, 258, 259, 260]
  end

  it "works with Integer ranges near the Fixnum limits" do
    (2**62-2..2**62).to_a.should == [2**62-2, 2**62-1, 2**62]
    (fixnum_max-1..fixnum_max).to_a.should == [fixnum_max-1, fixnum_max]
    (fixnum_max-1...fixnum_max).to_a.should == [fixnum_max-1]
    (fixnum_max-1..fixnum_max+1).to_a.should == [fixnum_max-1, fixnum_max, fixnum_max+1]
    (fixnum_min..fixnum_min+1).to_a.should == [fixnum_min, fixnum_min+1]
    (fixnum_min-1..fixnum_min+1).to_a.should == [fixnum_min-1, fixnum_min, fixnum_min+1]
  end

  it "returns a new mutable array each time" do
    a = (1..3).to_a
    a << 4
    a.should == [1, 2, 3, 4]
    (1..3).to_a.should == [1, 2, 3]
  end

  it "uses #each defined in a subclass" do
    subclass = Class.new(Range) do
      def each
        yield :a
        yield :b
      end
    end
    subclass.new(1, 3).to_a.should == [:a, :b]
  end

  it "uses #each aliased to another method in a subclass" do
    subclass = Class.new(Range) do
      alias_method :each, :reverse_each
    end
    subclass.new(1, 3).to_a.should == [3, 2, 1]
  end

  it "uses #each defined on the singleton class" do
    range = Class.new(Range).new(1, 3)
    def range.each
      yield :singleton
    end
    range.to_a.should == [:singleton]
  end

  it "works with Ranges of Symbols" do
    (:A..:z).to_a.size.should == 58
  end

  it "works for non-ASCII ranges" do
    ('Σ'..'Ω').to_a.should == ["Σ", "Τ", "Υ", "Φ", "Χ", "Ψ", "Ω"]
  end

  it "throws an exception for endless ranges" do
    -> { eval("(1..)").to_a }.should.raise(RangeError)
  end

  it "throws an exception for beginless ranges" do
    -> { (..1).to_a }.should.raise(TypeError)
  end
end
