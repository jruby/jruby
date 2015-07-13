require File.expand_path('../../../spec_helper', __FILE__)

describe "Process.waitpid" do
  it "needs to be reviewed for spec completeness"
  
  it "returns nil when the process has not yet completed and WNOHANG is specified" do
    pid = spawn("sleep 5")
    Process.waitpid(pid, Process::WNOHANG).should == nil
  end
end
