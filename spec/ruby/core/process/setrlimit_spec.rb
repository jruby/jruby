require File.expand_path('../../../spec_helper', __FILE__)

describe "Process.setrlimit and Process.getrlimit" do
  platform_is_not :windows do
    it "coerces arguments to Integers" do
      lim, max = Process.getrlimit(Process::RLIMIT_CORE)
      Process.setrlimit(mock_int(Process::RLIMIT_CORE),
                        mock_int(lim),
                        mock_int(max)).should be_nil
    end

    it "limit and get core size (bytes)" do
      lim, max = Process.getrlimit(Process::RLIMIT_CORE)
      lim.kind_of?(Integer).should == true
      max.kind_of?(Integer).should == true
      Process.setrlimit(Process::RLIMIT_CORE, lim, max).should be_nil
    end

    it "limit and get CPU time (seconds)" do
      lim, max = Process.getrlimit(Process::RLIMIT_CPU)
      lim.kind_of?(Integer).should == true
      max.kind_of?(Integer).should == true
      Process.setrlimit(Process::RLIMIT_CPU, lim, max).should be_nil
    end

    it "limit and get data segment (bytes)" do
      lim, max = Process.getrlimit(Process::RLIMIT_DATA)
      lim.kind_of?(Integer).should == true
      max.kind_of?(Integer).should == true
      Process.setrlimit(Process::RLIMIT_DATA, lim, max).should be_nil
    end

    it "limit and get file size (bytes)" do
      lim, max = Process.getrlimit(Process::RLIMIT_FSIZE)
      lim.kind_of?(Integer).should == true
      max.kind_of?(Integer).should == true
      Process.setrlimit(Process::RLIMIT_FSIZE, lim, max).should be_nil
    end

    it "limit and get file descriptors (number)" do
      lim, max = Process.getrlimit(Process::RLIMIT_NOFILE)
      lim.kind_of?(Integer).should == true
      max.kind_of?(Integer).should == true
      Process.setrlimit(Process::RLIMIT_NOFILE, lim, max).should be_nil
    end

    it "limit and get stack size (bytes)" do
      lim, max = Process.getrlimit(Process::RLIMIT_STACK)
      lim.kind_of?(Integer).should == true
      max.kind_of?(Integer).should == true
      Process.setrlimit(Process::RLIMIT_STACK, lim, max).should be_nil
    end

    platform_is_not :openbsd do
      it "limit and get total available memory (bytes)" do
        lim, max = Process.getrlimit(Process::RLIMIT_AS)
        lim.kind_of?(Integer).should == true
        max.kind_of?(Integer).should == true
        Process.setrlimit(Process::RLIMIT_AS, lim, max).should be_nil
      end
    end

    platform_is_not :solaris do
      it "limit and get total size for mlock(2) (bytes)" do
        lim, max = Process.getrlimit(Process::RLIMIT_MEMLOCK)
        lim.kind_of?(Integer).should == true
        max.kind_of?(Integer).should == true
        max = lim if lim > max # EINVAL is raised if this invariant is violated
        Process.setrlimit(Process::RLIMIT_MEMLOCK, lim, max).should be_nil
      end

      it "limit and get number of processes for the user (number)" do
        lim, max = Process.getrlimit(Process::RLIMIT_NPROC)
        lim.kind_of?(Integer).should == true
        max.kind_of?(Integer).should == true
        Process.setrlimit(Process::RLIMIT_NPROC, lim, max).should be_nil
      end

      it "limit and get resident memory size (bytes)" do
        lim, max = Process.getrlimit(Process::RLIMIT_RSS)
        lim.kind_of?(Integer).should == true
        max.kind_of?(Integer).should == true
        Process.setrlimit(Process::RLIMIT_RSS, lim, max).should be_nil
      end
    end

    platform_is :os => [:netbsd, :freebsd] do
      it "limit and get all socket buffers (bytes)" do
        lim, max = Process.getrlimit(Process::RLIMIT_SBSIZE)
        lim.kind_of?(Integer).should == true
        max.kind_of?(Integer).should == true
        Process.setrlimit(Process::RLIMIT_SBSIZE , lim, max).should be_nil
      end
    end
  end
end
