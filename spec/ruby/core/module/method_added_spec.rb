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
        alias_method :aliased_test, :test
        alias aliased_test2 test
      end

      $methods_added.should == [:test, :test2, :test, :aliased_test, :aliased_test2]
    ensure
      $methods_added = nil
    end
  end

  it "is not called when a method is undefined in self" do
    m = Module.new do
      def method_to_undef
      end

      def self.method_added(name)
        fail("method_added called by undef_method")
      end

      undef_method :method_to_undef
    end
    m.should_not have_method(:method_to_undef)
  end
end
