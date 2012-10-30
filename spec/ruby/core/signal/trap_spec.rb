require File.expand_path('../../../spec_helper', __FILE__)

describe "Signal.trap" do
  before :each do
    ScratchPad.clear

    @proc = lambda { ScratchPad.record :proc_trap }
    @saved_trap = Signal.trap(:HUP, @proc)
  end

  after :each do
    Signal.trap(:HUP, @saved_trap) if @saved_trap
  end

  it "returns the previous handler" do
    Signal.trap(:HUP, @saved_trap).should equal(@proc)
  end

  it "accepts a block in place of a proc/command argument" do
    Signal.trap(:HUP) { ScratchPad.record :block_trap }
    Process.kill :HUP, Process.pid
    sleep 0.5
    ScratchPad.recorded.should == :block_trap
  end

  ruby_version_is ""..."1.9" do
    it "ignores the signal when passed nil" do
      Signal.trap :HUP, nil
      Signal.trap(:HUP, @saved_trap).should == "IGNORE"
    end

    it "uses the command argument when passed both a command and block" do
      Signal.trap(:HUP, @proc) { ScratchPad.record :block_trap }
      Process.kill :HUP, Process.pid
      sleep 0.5
      ScratchPad.recorded.should == :proc_trap
    end
  end

  ruby_version_is "1.9" do
    it "ignores the signal when passed nil" do
      Signal.trap :HUP, nil
      Signal.trap(:HUP, @saved_trap).should be_nil
    end
  end

  it "accepts long names as Strings" do
    Signal.trap "SIGHUP", @proc
    Signal.trap("SIGHUP", @saved_trap).should equal(@proc)
  end

  it "acceps short names as Strings" do
    Signal.trap "HUP", @proc
    Signal.trap("HUP", @saved_trap).should equal(@proc)
  end

  it "accepts long names as Symbols" do
    Signal.trap :SIGHUP, @proc
    Signal.trap(:SIGHUP, @saved_trap).should equal(@proc)
  end

  it "accepts short names as Symbols" do
    Signal.trap :HUP, @proc
    Signal.trap(:HUP, @saved_trap).should equal(@proc)
  end

  it "accepts 'SIG_DFL' in place of a proc" do
    Signal.trap :HUP, "SIG_DFL"
    Signal.trap(:HUP, @saved_trap).should == "DEFAULT"
  end

  it "accepts 'DEFAULT' in place of a proc" do
    Signal.trap :HUP, "DEFAULT"
    Signal.trap(:HUP, @saved_trap).should == "DEFAULT"
  end

  it "accepts 'SIG_IGN' in place of a proc" do
    Signal.trap :HUP, "SIG_IGN"
    Signal.trap(:HUP, "SIG_IGN").should == "IGNORE"
  end

  it "accepts 'IGNORE' in place of a proc" do
    Signal.trap :HUP, "IGNORE"
    Signal.trap(:HUP, "IGNORE").should == "IGNORE"
  end

  describe "the special EXIT signal code" do

    it "accepts the EXIT code" do
      code = "trap(:EXIT, proc { print 1 })"
      ruby_exe(code).should == "1"
    end

    it "runs the proc before at_exit handlers" do
      code = "at_exit {print 1}; trap(:EXIT, proc {print 2}); at_exit {print 3}"
      ruby_exe(code).should == "231"
    end

    it "can unset the handler" do
      code = "trap(:EXIT, proc { print 1 }); trap(:EXIT, 'DEFAULT')"
      ruby_exe(code).should == ""
    end

  end

end
