require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)


ruby_version_is "1.8.7" do
  describe "Array#permutation" do

    before(:each) do
      @numbers = (1..3).to_a
      @yielded = []
    end

    it "returns an Enumerator of all permutations when called without a block or arguments" do
      enum = @numbers.permutation
      enum.should be_kind_of(Enumerable)
      enum.to_a.sort.should == [
        [1,2,3],[1,3,2],[2,1,3],[2,3,1],[3,1,2],[3,2,1]
      ].sort
    end

    it "returns an Enumerator of permutations of given length when called with an argument but no block" do
      enum = @numbers.permutation(1)
      enum.should be_kind_of(Enumerable)
      enum.to_a.sort.should == [[1],[2],[3]]
    end

    it "yields all permutations to the block then returns self when called with block but no arguments" do
      array = @numbers.permutation {|n| @yielded << n}
      array.should be_an_instance_of(Array)
      array.sort.should == @numbers.sort
      @yielded.sort.should == [
        [1,2,3],[1,3,2],[2,1,3],[2,3,1],[3,1,2],[3,2,1]
      ].sort
    end

    it "yields all permutations of given length to the block then returns self when called with block and argument" do
      array = @numbers.permutation(2) {|n| @yielded << n}
      array.should be_an_instance_of(Array)
      array.sort.should == @numbers.sort
      @yielded.sort.should == [[1,2],[1,3],[2,1],[2,3],[3,1],[3,2]].sort
    end

    it "returns the empty permutation ([[]]) when the given length is 0" do
      @numbers.permutation(0).to_a.should == [[]]
      @numbers.permutation(0) { |n| @yielded << n }
      @yielded.should == [[]]
    end

    it "returns the empty permutation([]) when called on an empty Array" do
      [].permutation.to_a.should == [[]]
      [].permutation { |n| @yielded << n }
      @yielded.should == [[]]
    end

    it "returns no permutations when the given length has no permutations" do
      @numbers.permutation(9).entries.size == 0
      @numbers.permutation(9) { |n| @yielded << n }
      @yielded.should == []
    end

    it "handles duplicate elements correctly" do
      @numbers << 1
      @numbers.permutation(2).sort.should == [
        [1,1],[1,1],[1,2],[1,2],[1,3],[1,3],
        [2,1],[2,1],[2,3],
        [3,1],[3,1],[3,2]
      ].sort
    end

    it "handles nested Arrays correctly" do
      # The ugliness is due to the order of permutations returned by
      # permutation being undefined combined with #sort croaking on Arrays of
      # Arrays.
      @numbers << [4,5]
      got = @numbers.permutation(2).to_a
      expected = [
         [1, 2],      [1, 3],      [1, [4, 5]],
         [2, 1],      [2, 3],      [2, [4, 5]],
         [3, 1],      [3, 2],      [3, [4, 5]],
        [[4, 5], 1], [[4, 5], 2], [[4, 5], 3]
      ]
      expected.each {|e| got.include?(e).should be_true}
      got.size.should == expected.size
    end

    it "truncates Float arguments" do
      @numbers.permutation(3.7).to_a.sort.should ==
        @numbers.permutation(3).to_a.sort
    end

    it "returns an Enumerator which works as expected even when the array was modified" do
      @numbers = [1, 2]
      enum = @numbers.permutation
      @numbers << 3
      enum.to_a.sort.should == [
        [1,2,3],[1,3,2],[2,1,3],[2,3,1],[3,1,2],[3,2,1]
      ].sort
    end

    it "generates from a defensive copy, ignoring mutations" do
      accum = []
      ary = [1,2,3]
      ary.permutation(3) do |x|
        accum << x
        ary[0] = 5
      end

      accum.should == [[1, 2, 3], [1, 3, 2], [2, 1, 3], [2, 3, 1], [3, 1, 2], [3, 2, 1]]
    end
  end
end
