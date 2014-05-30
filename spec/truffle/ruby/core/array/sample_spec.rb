require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Array#sample" do
  ruby_version_is "1.8.8" do
    it "selects a value from the array" do
      [4].sample.should eql(4)
    end

    it "returns a distribution of results" do
      source = [0,1,2,3,4]
      samples = ArraySpecs::SampleRange.collect { |el| source.sample }
      samples.uniq.sort.should eql(source)
    end

    it "returns nil for empty arrays" do
      [].sample.should be_nil
    end

    describe "passed a number n as an argument" do
      it "raises ArgumentError for a negative n" do
        lambda { [1, 2].sample(-1) }.should raise_error(ArgumentError)
      end

      it "selects values from the array" do
        source = [1,2,3,4]
        source.should include(*source.sample(2))
      end

      it "does not return the same value if the array is unique" do
        source = [1, 2, 3, 4]
        ArraySpecs::SampleCount.times do
          pair = source.sample(2)
          pair[0].should_not eql(pair[1])
        end
      end

      it "may return the same value if the array is not unique" do
        source = [4, 4]
        source.sample(2).should eql([4,4])
      end

      it "returns a distribution of results" do
        source = [0,1,2,3,4]
        samples = ArraySpecs::SampleRange.collect { |el| source.sample(3) }
        samples.flatten.uniq.sort.should eql(source)
      end

      it "tries to convert n to an Integer using #to_int" do
        a = [1, 2, 3, 4]
        a.sample(2.3).size.should == 2

        obj = mock('to_int')
        obj.should_receive(:to_int).and_return(2)
        a.sample(obj).size.should == 2
      end

      it "returns all values when n >= array size" do
        a = [1, 2, 3, 4]
        a.sample(4).sort.should == a
        a.sample(5).sort.should == a
      end

      it "returns [] for empty arrays or if n <= 0" do
        [].sample(1).should == []
        [1, 2, 3].sample(0).should == []
      end

      it "does not return subclass instances with Array subclass" do
        ArraySpecs::MyArray[1, 2, 3].sample(2).should be_an_instance_of(Array)
      end
    end
  end

  ruby_version_is "1.9" do
    it "returns only one element" do
      [2, 3].sample.should be_kind_of(Fixnum)
    end

    it "returns proper array size" do
      [2, 3].sample(2).size.should == 2
    end
  end

  ruby_version_is "1.9.3" do
    it "attempts coercion via #to_hash" do
      obj = mock('hash')
      obj.should_receive(:to_hash).once.and_return({})
      [2, 3].sample(obj)
    end

    it "uses default random generator" do
      Kernel.should_receive(:rand).and_return(1)
      [2, 3].sample(:random => Object.new)
    end

    it "uses given random generator" do
      random = Random.new
      random.should_receive(:rand).and_return(1)
      [2, 3].sample(:random => random)
    end

    it "uses default random generator and return proper array size" do
      Kernel.should_receive(:rand).and_return(1, 0)
      [2, 3].sample(2).should == [3, 2]
    end

    it "uses given random generator and return proper array size" do
      random = Random.new
      random.should_receive(:rand).and_return(1, 0)
      [2, 3].sample(2, :random => random).should == [3, 2]
    end
  end
end
