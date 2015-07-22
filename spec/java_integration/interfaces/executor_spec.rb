require File.dirname(__FILE__) + "/../spec_helper"

describe "java.util.concurrent.Executors" do

  EXECUTOR_TEST_VALUE = 101

  before do
    @executor = java.util.concurrent.Executors.newSingleThreadExecutor
  end

  after { @executor.shutdown }

  it "accepts a class that implements Callable interface" do
    cls = Class.new do
      include java.util.concurrent.Callable

      def call
        EXECUTOR_TEST_VALUE
      end
    end
    future = nil
    lambda { future = @executor.submit(cls.new) }.should_not raise_error
    future.get.should == EXECUTOR_TEST_VALUE
  end

end