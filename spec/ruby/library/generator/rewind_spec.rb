require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is ""..."1.9" do
  with_feature :generator do
    require File.expand_path('../fixtures/common', __FILE__)

    describe "Generator#rewind" do
      it "does nothing for empty generator" do
        g = GeneratorSpecs.empty
        g.index.should == 0
        g.rewind
        g.index.should == 0
      end

      it "rewinds the generator" do
        g = GeneratorSpecs.two_elems
        orig = g.next
        g.index.should == 1
        g.rewind
        g.index.should == 0
        g.next.should == orig
      end

      it "rewinds the previously finished generator" do
        g = GeneratorSpecs.two_elems
        g.next; g.next
        g.rewind
        g.end?.should == false
        g.next?.should == true
        g.next.should == 1
      end
    end
  end
end
