require File.dirname(__FILE__) + '/../../spec_helper'
require 'mspec/runner/formatters/spinner'
require 'mspec/runner/mspec'
require 'mspec/runner/example'

describe SpinnerFormatter, "#initialize" do
  it "permits zero arguments" do
    SpinnerFormatter.new
  end

  it "accepts one argument" do
    SpinnerFormatter.new nil
  end
end

describe SpinnerFormatter, "#register" do
  before :each do
    @formatter = SpinnerFormatter.new
    MSpec.stub(:register)
  end

  it "registers self with MSpec for appropriate actions" do
    MSpec.should_receive(:register).with(:start, @formatter)
    MSpec.should_receive(:register).with(:load, @formatter)
    MSpec.should_receive(:register).with(:after, @formatter)
    MSpec.should_receive(:register).with(:finish, @formatter)
    @formatter.register
  end

  it "creates TimerAction and TallyAction" do
    timer = double("timer")
    tally = double("tally")
    timer.should_receive(:register)
    tally.should_receive(:register)
    tally.should_receive(:counter)
    TimerAction.should_receive(:new).and_return(timer)
    TallyAction.should_receive(:new).and_return(tally)
    @formatter.register
  end
end

describe SpinnerFormatter, "#print" do
  after :each do
    $stdout = STDOUT
  end

  it "ignores the argument to #initialize and writes to $stdout" do
    $stdout = IOStub.new
    formatter = SpinnerFormatter.new "some/file"
    formatter.print "begonias"
    $stdout.should == "begonias"
  end
end

describe SpinnerFormatter, "#after" do
  before :each do
    $stdout = IOStub.new
    MSpec.stub(:retrieve).and_return(["a", "b"])
    @formatter = SpinnerFormatter.new
    @formatter.register
    @state = ExampleState.new("describe", "it")
  end

  after :each do
    $stdout = STDOUT
  end

  it "updates the spinner" do
    @formatter.start
    @formatter.load
    @formatter.after @state
    @formatter.after @state

    if ENV["TERM"] != "dumb"
      $stdout.should == "\r[/ | ========          20%                    | 00:00:00] " \
                        "\e[0;32m     0F \e[0;32m     0E\e[0m" \
                        "\r[- | ========          20%                    | 00:00:00] " \
                        "\e[0;32m     0F \e[0;32m     0E\e[0m"
    else
      $stdout.should == "\r[/ | ========          20%                    | 00:00:00] " \
                        "     0F      0E\r[- | ========          20%                 " \
                        "   | 00:00:00]      0F      0E"
    end
  end
end
