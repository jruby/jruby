require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is ""..."1.9" do
  with_feature :generator do
    require 'generator'
    describe "Generator.new" do
      it "creates a new generator from an Enumerable object" do
        g = Generator.new(['A', 'B', 'C', 'Z'])
        g.should_not == nil
        g.kind_of?(Generator).should == true
      end

      it "creates a new generator from a block" do
        g = Generator.new { |g|
          for i in 'A'..'C'
            g.yield i
          end
          g.yield 'Z'
        }

        g.should_not == nil
        g.kind_of?(Generator).should == true
      end
    end
  end
end
