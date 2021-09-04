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
    expect { future = @executor.submit(cls.new) }.not_to raise_error
    expect(future.get).to eq(EXECUTOR_TEST_VALUE)
  end

end