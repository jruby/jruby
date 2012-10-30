require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Thread#value" do
  it "returns the result of the block" do
    Thread.new { 3 }.value.should == 3
  end

  it "re-raises error for an uncaught exception" do
    t = Thread.new { raise "Hello" }
    lambda { t.value }.should raise_error(RuntimeError, "Hello")
  end

  ruby_version_is "" ... "1.9" do
    it "is false for a killed thread" do
      t = Thread.new { Thread.current.exit }
      t.value.should == false
    end
  end

  ruby_version_is "1.9" do
    it "is nil for a killed thread" do
      t = Thread.new { Thread.current.exit }
      t.value.should == nil
    end
  end

  ruby_version_is "" ... "1.9" do
    not_compliant_on :rubinius do
      it "is false for an uncaught exception thrown from a dying thread" do
        t = ThreadSpecs.dying_thread_ensures { 1/0 }
        t.value.should == false
      end
    end
  end
end
