describe :process_exit, :shared => true do
  it "raises a SystemExit with status 0" do
    lambda { @object.exit }.should raise_error(SystemExit) { |e|
      e.status.should == 0
    }
  end

  it "raises a SystemExit with the specified status" do
    [-2**16, -2**8, -8, -1, 0, 1 , 8, 2**8, 2**16].each do |value|
      lambda { @object.exit(value) }.should raise_error(SystemExit) { |e|
        e.status.should == value
      }
    end
  end

  it "raises a SystemExit with the specified boolean status" do
    { true => 0, false => 1 }.each do |value, status|
      lambda { @object.exit(value) }.should raise_error(SystemExit) { |e|
        e.status.should == status
      }
    end
  end

  it "tries to convert the passed argument to an Integer using #to_int" do
    obj = mock('5')
    obj.should_receive(:to_int).and_return(5)
    lambda { @object.exit(obj) }.should raise_error(SystemExit) { |e|
      e.status.should == 5
    }
  end

  it "converts the passed Float argument to an Integer" do
    { -2.2 => -2, -0.1 => 0, 5.5 => 5, 827.999 => 827 }.each do |value, status|
      lambda { @object.exit(value) }.should raise_error(SystemExit) { |e|
        e.status.should == status
      }
    end
  end

  it "raises TypeError if can't convert the argument to an Integer" do
    lambda { @object.exit(Object.new) }.should raise_error(TypeError)
    lambda { @object.exit('0') }.should raise_error(TypeError)
    lambda { @object.exit([0]) }.should raise_error(TypeError)
    lambda { @object.exit(nil) }.should raise_error(TypeError)
  end
end

describe :process_exit!, :shared => true do
  platform_is_not :windows do
    it "exits with the given status" do
      pid = Process.fork { @object.exit!(1) }
      pid, status = Process.waitpid2(pid)
      status.exitstatus.should == 1
    end

    it "exits immediately when called from a thread" do
      pid = Process.fork do
        Thread.new { @object.exit!(1) }.join

        # If the above doesn't instantly terminate the process our exit status
        # would be "2", leading to the test below to fail.
        Process.exit!(2)
      end

      pid, status = Process.waitpid2(pid)
      status.exitstatus.should == 1
    end
  end
end
