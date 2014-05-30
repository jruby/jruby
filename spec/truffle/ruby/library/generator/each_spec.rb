require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is ""..."1.9" do
  with_feature :generator do

    require File.expand_path('../fixtures/common', __FILE__)

    describe "Generator#each" do
      it "enumerates the elements" do
        g = GeneratorSpecs.four_elems
        result = []

        g.each { |element|
          result << element
        }

        result.should == ['A', 'B', 'C', 'Z']
      end

      it "rewinds the generator and only then enumerates the elements" do
        g = GeneratorSpecs.four_elems
        g.next; g.next
        result = []

        g.each { |element|
          result << element
        }

        result.should == ['A', 'B', 'C', 'Z']
      end
    end
  end
end
