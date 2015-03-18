require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is ""..."1.9" do
  with_feature :generator do
    require File.expand_path('../fixtures/common', __FILE__)

    describe "Generator#next?" do
      it "returns false for empty generator" do
        GeneratorSpecs.empty.next?.should == false
      end

      it "returns true for non-empty generator" do
        g = Generator.new([1])
        g.next?.should == true

        GeneratorSpecs.two_elems.next?.should == true

        g = Generator.new(['A', 'B', 'C', 'D', 'E', 'F'])
        g.next?.should == true
      end

      it "returns true if the generator has not reached the end yet" do
        g = GeneratorSpecs.two_elems
        g.next
        g.next?.should == true
      end

      it "returns false if the generator has reached the end" do
        g = GeneratorSpecs.two_elems
        g.next
        g.next
        g.next?.should == false
      end

      it "returns false if end? returns true" do
        g = GeneratorSpecs.two_elems
        def g.end?; true end
        g.next?.should == false
      end
    end

    describe "Generator#next" do
      it "raises an EOFError on empty generator" do
        lambda { GeneratorSpecs.empty.next }.should raise_error(EOFError)
      end

      it "raises an EOFError if no elements available" do
        g = GeneratorSpecs.two_elems
        g.next;
        g.next
        lambda { g.next }.should raise_error(EOFError)
      end

      it "raises an EOFError if end? returns true" do
        g = GeneratorSpecs.two_elems
        def g.end?; true end
        lambda { g.next }.should raise_error(EOFError)
      end

      it "returns the element at the current position and moves forward" do
        g = GeneratorSpecs.two_elems
        g.index.should == 0
        g.next.should == 1
        g.index.should == 1
      end

      it "subsequent calls should return all elements in proper order" do
        g = GeneratorSpecs.four_elems

        result = []
        while g.next?
          result << g.next
        end

        result.should == ['A', 'B', 'C', 'Z']
      end
    end
  end
end
