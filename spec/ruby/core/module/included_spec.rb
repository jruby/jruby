require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Module#included" do
  it "is invoked when self is included in another module or class" do
    begin
      m = Module.new do
        def self.included(o)
          $included_by = o
        end
      end

      c = Class.new { include m }

      $included_by.should == c
    ensure
      $included_by = nil
    end
  end

  it "allows extending self with the object into which it is being included" do
    m = Module.new do
      def self.included(o)
        o.extend(self)
      end

      def test
        :passed
      end
    end

    c = Class.new{ include(m) }
    c.test.should == :passed
  end

  ruby_version_is ""..."1.9" do
    it "is private in its default implementation" do
      Module.new.private_methods.should include("included")
    end
  end

  ruby_version_is "1.9" do
    it "is private in its default implementation" do
      Module.new.private_methods.should include(:included)
    end
  end
end
