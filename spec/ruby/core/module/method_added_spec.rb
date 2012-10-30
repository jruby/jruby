require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Module#method_added" do
  it "is a private instance method" do
    Module.should have_private_instance_method(:method_added)
  end

  it "returns nil in the default implementation" do
    Module.new do
      method_added(:test).should == nil
    end
  end

  it "is called when a new method is defined in self" do
    begin
      $methods_added = []

      m = Module.new do
        def self.method_added(name)
          $methods_added << name
        end

        def test() end
        def test2() end
        def test() end
      end

      $methods_added.should == [:test,:test2, :test]
    ensure
      $methods_added = nil
    end
  end
end
