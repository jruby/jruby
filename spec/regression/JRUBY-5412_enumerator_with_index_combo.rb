# JRUBY-5412
# Test Array and Enumerable methods which can take an argument and
# returns an Enumerator, and test whether we can call with_index
# on the resultant enumerator.

# Note that we are using Set equality in some cases; with_index returns
# an array, and while the expected return values are correct as array,
# logically speaking the order can change without losing correctness.

require 'set'

describe "Enumerator#combination" do
  it "returns Enumerator that can call #with_index" do
    a = []
    [1, 2, 3].combination(2).with_index {|v, i| a << [v, i]}
    a.to_set.should == Set.new([[[1, 2], 0], [[1, 3], 1], [[2, 3], 2]])
  end
end

describe "Enumerator#cycle" do
  it "returns Enumerator that can call #with_index" do
    a = []
    [1, 2, 3, 4].cycle(2).with_index {|v, i| a << [v, i]}
    a.should == [[1, 0], [2, 1], [3, 2], [4, 3], [1, 4], [2, 5], [3, 6], [4, 7]]
  end
end

describe "Enumerator#each_slice" do
  it "returns Enumerator that can call #with_index" do
    a = []
    [1, 2, 3, 4].each_slice(2).with_index {|v, i| a << [v, i]}
    a.should == [[[1, 2], 0], [[3, 4], 1]]
  end
end

describe "Enumerator#each_cons" do
  it "returns Enumerator that can call #with_index" do
    a = []
    [1, 2, 3, 4].each_cons(2).with_index {|v, i| a << [v, i]}
    a.should == [[[1, 2], 0], [[2, 3], 1], [[3, 4], 2]]
  end
end

describe "Enumerator#permutation" do
  it "returns Enumerator that can call #with_index" do
    a = []
    [1, 2, 3, 4].permutation(2).with_index {|v, i| a << [v, i]}
    a.to_set.should == Set.new(
                [[[1, 2], 0], [[1, 3],  1], [[1, 4],  2],
                 [[2, 1], 3], [[2, 3],  4], [[2, 4],  5],
                 [[3, 1], 6], [[3, 2],  7], [[3, 4],  8],
                 [[4, 1], 9], [[4, 2], 10], [[4, 3], 11]])
  end
end

if RUBY_VERSION >= "1.9.2" 
  describe "Enumerator#slice_before" do
    it "returns Enumerator that can call #with_index" do
      a = []
      ("a".."e").to_a.slice_before(/c|d/).with_index {|v, i| a << [v, i]}
      a.should == [[["a", "b"], 0], [["c"], 1], [["d", "e"], 2]]
    end
  end

  describe "Enumerator#repeated_combination" do
    it "returns Enumerator that can call #with_index" do
      a = []
      [1, 2, 3, 4].repeated_combination(2).with_index {|v, i| a << [v, i]}
      a.to_set.should == Set.new(
      [[[1, 1], 0], [[1, 2], 1], [[1, 3], 2], [[1, 4], 3],
      [[2, 2], 4], [[2, 3], 5], [[2, 4], 6],
      [[3, 3], 7], [[3, 4], 8],
      [[4, 4], 9]])
    end
  end
end