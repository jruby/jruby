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

    expect(Foo.new.foo).to eq(nil)
  end
end

describe "A class which has been set persistent" do
  it "retains identity and instance vars even if the object leaves Ruby" do
    # Note: this will always pass in JRuby 1.7, since all objects are still persistent
    java.util.ArrayList.__persistent__ = true
    al = java.util.ArrayList.new
    al.instance_variable_set :@foo, 1

    al2 = ReceivesAndReturnsObject.returnObject(al)

    expect(al2).to be_equal(al)
    expect(al2.instance_variable_get(:@foo)).to eq(1)
  end
end

describe "A class which has not been set persistent" do
  it "warns when the first instance variable is set" do
    warns = with_warn_captured {
      java.lang.Object.new.instance_variable_set :@foo, 1
    }
    warns.first.first.should =~ /instance vars on non-persistent Java type/

    java.lang.Object.__persistent__ = false
  end
end

