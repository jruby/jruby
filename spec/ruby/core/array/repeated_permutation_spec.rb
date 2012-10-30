require File.expand_path('../../../spec_helper', __FILE__)


ruby_version_is "1.9" do
  describe "Array#repeated_permutation" do

    before(:each) do
      @numbers = [10, 11, 12]
      @permutations = [[10, 10], [10, 11], [10, 12], [11, 10], [11, 11], [11, 12], [12, 10], [12, 11], [12, 12]]
    end

    it "returns an Enumerator of all repeated permutations of given length when called without a block" do
      enum = @numbers.repeated_permutation(2)
      enum.should be_kind_of(Enumerable)
      enum.to_a.sort.should == @permutations
    end

    it "yields all repeated_permutations to the block then returns self when called with block but no arguments" do
      yielded = []
      @numbers.repeated_permutation(2) {|n| yielded << n}.should equal(@numbers)
      yielded.sort.should == @permutations
    end

    it "yields the empty repeated_permutation ([[]]) when the given length is 0" do
      @numbers.repeated_permutation(0).to_a.should == [[]]
      [].repeated_permutation(0).to_a.should == [[]]
    end

    it "does not yield when called on an empty Array with a nonzero argument" do
      [].repeated_permutation(10).to_a.should == []
    end

    it "handles duplicate elements correctly" do
      @numbers[-1] = 10
      @numbers.repeated_permutation(2).sort.should ==
        [[10, 10], [10, 10], [10, 10], [10, 10], [10, 11], [10, 11], [11, 10], [11, 10], [11, 11]]
    end

    it "truncates Float arguments" do
      @numbers.repeated_permutation(3.7).to_a.sort.should ==
        @numbers.repeated_permutation(3).to_a.sort
    end

    it "returns an Enumerator which works as expected even when the array was modified" do
      @numbers.shift
      enum = @numbers.repeated_permutation(2)
      @numbers.unshift 10
      enum.to_a.sort.should == @permutations
    end

    it "allows permutations larger than the number of elements" do
      [1,2].repeated_permutation(3).sort.should ==
        [[1, 1, 1], [1, 1, 2], [1, 2, 1],
         [1, 2, 2], [2, 1, 1], [2, 1, 2],
         [2, 2, 1], [2, 2, 2]]
    end

    it "generates from a defensive copy, ignoring mutations" do
      accum = []
      ary = [1,2]
      ary.repeated_permutation(3) do |x|
        accum << x
        ary[0] = 5
      end

      accum.should ==
        [[1, 1, 1], [1, 1, 2], [1, 2, 1],
         [1, 2, 2], [2, 1, 1], [2, 1, 2],
         [2, 2, 1], [2, 2, 2]]
    end
  end
end
