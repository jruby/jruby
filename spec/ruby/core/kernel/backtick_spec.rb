require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Kernel#`" do
  it "is a private method" do
    Kernel.should have_private_instance_method(:`)
  end

  it "returns the standard output of the executed sub-process" do
    ip = 'world'
    `echo disc #{ip}`.should == "disc world\n"
  end

  it "tries to convert the given argument to String using #to_str" do
    (obj = mock('echo test')).should_receive(:to_str).and_return("echo test")
    Kernel.`(obj).should == "test\n"
  end

  platform_is_not :windows do
    it "sets $? to the exit status of the executed sub-process" do
      ip = 'world'
      `echo disc #{ip}`
      $?.should be_kind_of(Process::Status)
      $?.stopped?.should == false
      $?.exited?.should == true
      $?.exitstatus.should == 0
      $?.success?.should == true
      `echo disc #{ip}; exit 99`
      $?.should be_kind_of(Process::Status)
      $?.stopped?.should == false
      $?.exited?.should == true
      $?.exitstatus.should == 99
      $?.success?.should == false
    end
  end

  platform_is :windows do
    it "sets $? to the exit status of the executed sub-process" do
      ip = 'world'
      `echo disc #{ip}`
      $?.should be_kind_of(Process::Status)
      $?.stopped?.should == false
      $?.exited?.should == true
      $?.exitstatus.should == 0
      $?.success?.should == true
      `echo disc #{ip}& exit 99`
      $?.should be_kind_of(Process::Status)
      $?.stopped?.should == false
      $?.exited?.should == true
      $?.exitstatus.should == 99
      $?.success?.should == false
    end
  end
end

describe "Kernel.`" do
  it "needs to be reviewed for spec completeness"
end
