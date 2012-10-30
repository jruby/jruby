require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "Array#repeated_combination" do
    before :each do
      @array = [10, 11, 12]
    end

    it "returns an enumerator when no block is provided" do
      @array.repeated_combination(2).should be_an_instance_of(enumerator_class)
    end

    it "returns self when a block is given" do
      @array.repeated_combination(2){}.should equal(@array)
    end

    it "yields nothing for negative length and return self" do
      @array.repeated_combination(-1){ fail }.should equal(@array)
      @array.repeated_combination(-10){ fail }.should equal(@array)
    end

    it "yields the expected repeated_combinations" do
      @array.repeated_combination(2).to_a.sort.should == [[10, 10], [10, 11], [10, 12], [11, 11], [11, 12], [12, 12]]
      @array.repeated_combination(3).to_a.sort.should == [[10, 10, 10], [10, 10, 11], [10, 10, 12], [10, 11, 11], [10, 11, 12],
                                                          [10, 12, 12], [11, 11, 11], [11, 11, 12], [11, 12, 12], [12, 12, 12]]
    end

    it "yields [] when length is 0" do
      @array.repeated_combination(0).to_a.should == [[]] # one repeated_combination of length 0
      [].repeated_combination(0).to_a.should == [[]] # one repeated_combination of length 0
    end

    it "yields nothing when the array is empty and num is non zero" do
      [].repeated_combination(5).to_a.should == [] # one repeated_combination of length 0
    end

    it "yields a partition consisting of only singletons" do
      @array.repeated_combination(1).sort.to_a.should == [[10],[11],[12]]
    end

    it "accepts sizes larger than the original array" do
      @array.repeated_combination(4).to_a.sort.should ==
        [[10, 10, 10, 10], [10, 10, 10, 11], [10, 10, 10, 12],
         [10, 10, 11, 11], [10, 10, 11, 12], [10, 10, 12, 12],
         [10, 11, 11, 11], [10, 11, 11, 12], [10, 11, 12, 12],
         [10, 12, 12, 12], [11, 11, 11, 11], [11, 11, 11, 12],
         [11, 11, 12, 12], [11, 12, 12, 12], [12, 12, 12, 12]]
    end

    it "generates from a defensive copy, ignoring mutations" do
      accum = []
      @array.repeated_combination(2) do |x|
        accum << x
        @array[0] = 1
      end
      accum.should == [[10, 10], [10, 11], [10, 12], [11, 11], [11, 12], [12, 12]]
    end
  end
end
