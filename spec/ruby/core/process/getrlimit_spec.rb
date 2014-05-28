require File.expand_path('../../../spec_helper', __FILE__)

platform_is_not :windows do
  describe "Process.getrlimit" do
    it "returns a two-element Array of Integers" do
      result = Process.getrlimit Process::RLIMIT_CORE
      result.size.should == 2
      result.first.should be_kind_of(Integer)
      result.last.should be_kind_of(Integer)
    end

    context "when passed an Object" do
      before do
        @resource = Process::RLIMIT_CORE
      end

      it "calls #to_int to convert to an Integer" do
        obj = mock("process getrlimit integer")
        obj.should_receive(:to_int).and_return(@resource)

        Process.getrlimit(obj).should == Process.getrlimit(@resource)
      end

      it "raises a TypeError if #to_int does not return an Integer" do
        obj = mock("process getrlimit integer")
        obj.should_receive(:to_int).and_return(nil)

        lambda { Process.getrlimit(obj) }.should raise_error(TypeError)
      end
    end

    context "when passed a Symbol" do

      platform_is_not :openbsd do
        it "coerces :AS into RLIMIT_AS" do
          Process.getrlimit(:AS).should == Process.getrlimit(Process::RLIMIT_AS)
        end
      end

      it "coerces :CORE into RLIMIT_CORE" do
        Process.getrlimit(:CORE).should == Process.getrlimit(Process::RLIMIT_CORE)
      end

      it "coerces :CPU into RLIMIT_CPU" do
        Process.getrlimit(:CPU).should == Process.getrlimit(Process::RLIMIT_CPU)
      end

      it "coerces :DATA into RLIMIT_DATA" do
        Process.getrlimit(:DATA).should == Process.getrlimit(Process::RLIMIT_DATA)
      end

      it "coerces :FSIZE into RLIMIT_FSIZE" do
        Process.getrlimit(:FSIZE).should == Process.getrlimit(Process::RLIMIT_FSIZE)
      end

      it "coerces :NOFILE into RLIMIT_NOFILE" do
        Process.getrlimit(:NOFILE).should == Process.getrlimit(Process::RLIMIT_NOFILE)
      end

      it "coerces :STACK into RLIMIT_STACK" do
        Process.getrlimit(:STACK).should == Process.getrlimit(Process::RLIMIT_STACK)
      end

      platform_is_not :solaris do
        it "coerces :MEMLOCK into RLIMIT_MEMLOCK" do
          Process.getrlimit(:MEMLOCK).should == Process.getrlimit(Process::RLIMIT_MEMLOCK)
        end

        it "coerces :NPROC into RLIMIT_NPROC" do
          Process.getrlimit(:NPROC).should == Process.getrlimit(Process::RLIMIT_NPROC)
        end

        it "coerces :RSS into RLIMIT_RSS" do
          Process.getrlimit(:RSS).should == Process.getrlimit(Process::RLIMIT_RSS)
        end
      end

      platform_is :os => [:netbsd, :freebsd] do
        it "coerces :SBSIZE into RLIMIT_SBSIZE" do
          Process.getrlimit(:SBSIZE).should == Process.getrlimit(Process::RLIMIT_SBSIZE)
        end
      end

      platform_is :linux do
        it "coerces :RTPRIO into RLIMIT_RTPRIO" do
          Process.getrlimit(:RTPRIO).should == Process.getrlimit(Process::RLIMIT_RTPRIO)
        end

        it "coerces :RTTIME into RLIMIT_RTTIME" do
          Process.getrlimit(:RTTIME).should == Process.getrlimit(Process::RLIMIT_RTTIME)
        end

        it "coerces :SIGPENDING into RLIMIT_SIGPENDING" do
          Process.getrlimit(:SIGPENDING).should == Process.getrlimit(Process::RLIMIT_SIGPENDING)
        end

        it "coerces :MSGQUEUE into RLIMIT_MSGQUEUE" do
          Process.getrlimit(:MSGQUEUE).should == Process.getrlimit(Process::RLIMIT_MSGQUEUE)
        end

        it "coerces :NICE into RLIMIT_NICE" do
          Process.getrlimit(:NICE).should == Process.getrlimit(Process::RLIMIT_NICE)
        end
      end

      it "raises ArgumentError when passed an unknown resource" do
        lambda { Process.getrlimit(:FOO) }.should raise_error(ArgumentError)
      end
    end

    context "when passed a String" do

      platform_is_not :openbsd do
        it "coerces 'AS' into RLIMIT_AS" do
          Process.getrlimit("AS").should == Process.getrlimit(Process::RLIMIT_AS)
        end
      end

      it "coerces 'CORE' into RLIMIT_CORE" do
        Process.getrlimit("CORE").should == Process.getrlimit(Process::RLIMIT_CORE)
      end

      it "coerces 'CPU' into RLIMIT_CPU" do
        Process.getrlimit("CPU").should == Process.getrlimit(Process::RLIMIT_CPU)
      end

      it "coerces 'DATA' into RLIMIT_DATA" do
        Process.getrlimit("DATA").should == Process.getrlimit(Process::RLIMIT_DATA)
      end

      it "coerces 'FSIZE' into RLIMIT_FSIZE" do
        Process.getrlimit("FSIZE").should == Process.getrlimit(Process::RLIMIT_FSIZE)
      end

      it "coerces 'NOFILE' into RLIMIT_NOFILE" do
        Process.getrlimit("NOFILE").should == Process.getrlimit(Process::RLIMIT_NOFILE)
      end

      it "coerces 'STACK' into RLIMIT_STACK" do
        Process.getrlimit("STACK").should == Process.getrlimit(Process::RLIMIT_STACK)
      end

      platform_is_not :solaris do
        it "coerces 'MEMLOCK' into RLIMIT_MEMLOCK" do
          Process.getrlimit("MEMLOCK").should == Process.getrlimit(Process::RLIMIT_MEMLOCK)
        end

        it "coerces 'NPROC' into RLIMIT_NPROC" do
          Process.getrlimit("NPROC").should == Process.getrlimit(Process::RLIMIT_NPROC)
        end

        it "coerces 'RSS' into RLIMIT_RSS" do
          Process.getrlimit("RSS").should == Process.getrlimit(Process::RLIMIT_RSS)
        end
      end

      platform_is :os => [:netbsd, :freebsd] do
        it "coerces 'SBSIZE' into RLIMIT_SBSIZE" do
          Process.getrlimit("SBSIZE").should == Process.getrlimit(Process::RLIMIT_SBSIZE)
        end
      end

      platform_is :linux do
        it "coerces 'RTPRIO' into RLIMIT_RTPRIO" do
          Process.getrlimit("RTPRIO").should == Process.getrlimit(Process::RLIMIT_RTPRIO)
        end

        it "coerces 'RTTIME' into RLIMIT_RTTIME" do
          Process.getrlimit("RTTIME").should == Process.getrlimit(Process::RLIMIT_RTTIME)
        end

        it "coerces 'SIGPENDING' into RLIMIT_SIGPENDING" do
          Process.getrlimit("SIGPENDING").should == Process.getrlimit(Process::RLIMIT_SIGPENDING)
        end

        it "coerces 'MSGQUEUE' into RLIMIT_MSGQUEUE" do
          Process.getrlimit("MSGQUEUE").should == Process.getrlimit(Process::RLIMIT_MSGQUEUE)
        end

        it "coerces 'NICE' into RLIMIT_NICE" do
          Process.getrlimit("NICE").should == Process.getrlimit(Process::RLIMIT_NICE)
        end
      end

      it "raises ArgumentError when passed an unknown resource" do
        lambda { Process.getrlimit("FOO") }.should raise_error(ArgumentError)
      end
    end

    context "when passed on Object" do
      before do
        @resource = Process::RLIMIT_CORE
      end

      it "calls #to_str to convert to a String" do
        obj = mock("process getrlimit string")
        obj.should_receive(:to_str).and_return("CORE")
        obj.should_not_receive(:to_int)

        Process.getrlimit(obj).should == Process.getrlimit(@resource)
      end

      it "calls #to_int if #to_str does not return a String" do
        obj = mock("process getrlimit string")
        obj.should_receive(:to_str).and_return(nil)
        obj.should_receive(:to_int).and_return(@resource)

        Process.getrlimit(obj).should == Process.getrlimit(@resource)
      end
    end
  end
end
