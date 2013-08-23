require 'jruby'
require 'jruby/synchronized'

describe "JRuby::Synchronized" do
  before(:each) do
    @cls = Class.new do
      include RSpec::Matchers

      def call_wait
        JRuby.reference(self).wait(1)
      end

      def expect_synchronized
        expect { call_wait }.not_to raise_error
      end

      def expect_unsynchronized
        expect { call_wait }.to raise_error(java.lang.IllegalMonitorStateException)
      end
    end
  end

  it "should make methods on instances of synchronized classes synchronized" do
    @cls.new.expect_unsynchronized
    @cls.class_eval { include JRuby::Synchronized }
    @cls.new.expect_synchronized
  end

  it "should also affect subclasses" do
    subcls = Class.new(@cls)
    subcls.new.expect_unsynchronized
    subcls.class_eval { include JRuby::Synchronized }
    subcls.new.expect_synchronized
  end

  it "should not affect superclasses" do
    @cls.new.expect_unsynchronized
    subcls = Class.new(@cls) { include JRuby::Synchronized }
    @cls.new.expect_unsynchronized
  end

  it "should affect existing instances" do
    instance = @cls.new
    instance.expect_unsynchronized
    @cls.class_eval { include JRuby::Synchronized }
    instance.expect_synchronized
  end

  it "should affect existing subclass instances" do
    subcls = Class.new(@cls)
    instance = subcls.new
    instance.expect_unsynchronized
    @cls.class_eval { include JRuby::Synchronized }
    instance.expect_synchronized
  end

  it "should work for singleton classes" do
    instance = @cls.new
    instance.expect_unsynchronized
    instance.extend JRuby::Synchronized
    instance.expect_synchronized
  end

  it "should be includable only in classes" do
    mod = Module.new
    lambda { mod.class_eval { include JRuby::Synchronized } }.should raise_error(TypeError)
  end
end
