require File.expand_path('../../../spec_helper', __FILE__)

describe "Process.setpgid" do
  with_feature :fork do
    it "sets the process group id of the specified process" do
      rd, wr = IO.pipe

      pid = Process.fork do
        wr.close
        rd.read
        rd.close
        Process.exit!
      end

      rd.close

      Process.getpgid(pid).should == Process.getpgrp
      Process.setpgid(mock_int(pid), mock_int(pid)).should == 0
      Process.getpgid(pid).should == pid

      wr.write ' '
      wr.close
    end
  end
end
