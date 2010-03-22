require File.dirname(__FILE__) + "/../spec_helper"

TEST_VALUE = 101

class MyCallableTask
  include java.util.concurrent.Callable
  
  def call
    sleep 2
    TEST_VALUE
  end
  
end

describe "java.util.concurrent.Executors" do
  before do
    @executor = java.util.concurrent.Executors.newSingleThreadExecutor
  end
  
  it "accepts a class that implements Callable interface" do
    pending "this regression is described in JRUBY-4631" do
      lambda { @future_callable = @executor.submit(MyCallableTask.new) }.should_not raise_error(TypeError)
    end
    @future.get.should == TEST_VALUE
  end
  
  after do
    @executor.shutdown
  end
  
end