require File.expand_path('../../../spec_helper', __FILE__)

describe "Process.wait" do
  before :all do
    begin
      leaked = Process.waitall
      puts "leaked before wait specs: #{leaked}" unless leaked.empty?
    rescue NotImplementedError
    end
  end

  it "raises an Errno::ECHILD if there are no child processes" do
    lambda { Process.wait }.should raise_error(Errno::ECHILD)
  end

  platform_is_not :windows do
    it "returns its childs pid" do
      pid = Process.fork { Process.exit! }
      Process.wait.should == pid
    end

    it "sets $? to a Process::Status" do
      pid = Process.fork { Process.exit! }
      Process.wait
      $?.should be_kind_of(Process::Status)
      $?.pid.should == pid
    end

    it "waits for any child process if no pid is given" do
      pid = Process.fork { Process.exit! }
      Process.wait.should == pid
      lambda { Process.kill(0, pid) }.should raise_error(Errno::ESRCH)
    end

    it "waits for a specific child if a pid is given" do
      pid1 = Process.fork { Process.exit! }
      pid2 = Process.fork { Process.exit! }
      Process.wait(pid2).should == pid2
      Process.wait(pid1).should == pid1
      lambda { Process.kill(0, pid1) }.should raise_error(Errno::ESRCH)
      lambda { Process.kill(0, pid2) }.should raise_error(Errno::ESRCH)
    end

    it "coerces the pid to an Integer" do
      pid1 = Process.fork { Process.exit! }
      Process.wait(mock_int(pid1)).should == pid1
      lambda { Process.kill(0, pid1) }.should raise_error(Errno::ESRCH)
    end

    # This spec is probably system-dependent.
    it "waits for a child whose process group ID is that of the calling process" do
      read, write = IO.pipe
      pid1 = Process.fork {
        read.close
        Process.setpgid(0, 0)
        write << 1
        write.close
        Process.exit!
      }
      Process.setpgid(0, 0)
      ppid = Process.pid
      pid2 = Process.fork {
        read.close
        Process.setpgid(0, ppid);
        write << 2
        write.close
        Process.exit!
      }

      write.close
      read.read(1)
      read.read(1) # to give children a chance to set their process groups
      read.close
      Process.wait(0).should == pid2
      Process.wait.should == pid1
    end

    # This spec is probably system-dependent.
    it "doesn't block if no child is available when WNOHANG is used" do
      read, write = IO.pipe
      pid = Process.fork do
        read.close
        Signal.trap("TERM") { Process.exit! }
        write << 1
        write.close
        sleep
      end

      Process.wait(pid, Process::WNOHANG).should be_nil

      # wait for the child to setup its TERM handler
      write.close
      read.read(1)
      read.close

      Process.kill("TERM", pid)
      Process.wait.should == pid
    end

    it "always accepts flags=0" do
      pid = Process.fork { Process.exit! }
      Process.wait(-1, 0).should == pid
      lambda { Process.kill(0, pid) }.should raise_error(Errno::ESRCH)
    end
  end
end
