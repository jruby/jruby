require File.dirname(__FILE__) + "/../spec_helper"

EXECUTOR_TEST_VALUE = 101

describe "java.util.concurrent.Executors" do
  before do
    @executor = java.util.concurrent.Executors.newSingleThreadExecutor
  end
  
  it "accepts a class that implements Callable interface" do
    cls = Class.new do
      include java.util.concurrent.Callable

      def call
        EXECUTOR_TEST_VALUE
      end
    end
    lambda { @future = @executor.submit(cls.new) }.should_not raise_error(TypeError)
    @future.get.should == EXECUTOR_TEST_VALUE
  end
  
  after do
    @executor.shutdown
  end
  
end