require File.expand_path('../../../spec_helper', __FILE__)

describe "Process.euid" do
  it "returns the effective user ID for this process" do
    Process.euid.should be_kind_of(Fixnum)
  end

  it "also goes by Process::UID.eid" do
    Process::UID.eid.should == Process.euid
  end

  it "also goes by Process::Sys.geteuid" do
    Process::Sys.geteuid.should == Process.euid
  end
end

describe "Process.euid=" do
  it "needs to be reviewed for spec completeness"
end
