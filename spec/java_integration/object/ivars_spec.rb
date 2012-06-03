require 'java'

java_import 'java_integration.fixtures.ReceivesAndReturnsObject'

describe "An object which extends a Java class" do
  it "should not explode when defined? is called on non-existent ivar" do
    java_import java.util.Hashtable

    class Foo < Hashtable
      def foo
        @a = 1  # Have one ivar set so we don't hit different code path
        defined? @foo # This should
      end
    end

    Foo.new.foo.should == nil
  end
end

describe "A class which has been set persistent" do
  it "retains identity and instance vars even if the object leaves Ruby" do
    # Note: this will always pass in JRuby 1.7, since all objects are still persistent
    java.util.ArrayList.__persistent__ = true
    al = java.util.ArrayList.new
    al.instance_variable_set :@foo, 1

    al2 = ReceivesAndReturnsObject.returnObject(al)

    al2.should be_equal(al)
    al2.instance_variable_get(:@foo).should == 1
  end
end

