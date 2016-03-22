require File.expand_path('../../../spec_helper', __FILE__)

describe "Thread#backtrace" do
  it "needs to be reviewed for spec completeness"

  it "returns current backtrace of a thread" do

    t = Thread.new do
      begin
        sleep
      rescue
      end
    end

    Thread.pass while t.status != 'sleep'

    backtrace = t.backtrace
    backtrace.should be_kind_of(Array)
    backtrace.first.should =~ /`sleep'/

    t.raise 'finish the thread'
    t.join
  end

  it "returns nil for dead thread" do
    t = Thread.new {}
    t.join
    t.backtrace.should == nil
  end
end
