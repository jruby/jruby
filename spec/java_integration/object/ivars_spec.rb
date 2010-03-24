require 'java'

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

