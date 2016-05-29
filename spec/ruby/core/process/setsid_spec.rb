require File.expand_path('../../../spec_helper', __FILE__)

describe "Process.setsid" do
  with_feature :fork do
    it "establishes this process as a new session and process group leader" do
      read, write = IO.pipe
      read2, write2 = IO.pipe
      pid = Process.fork {
        begin
          read.close
          write2.close
          pgid = Process.setsid
          write << pgid
          write.close
          read2.gets
        rescue Exception => e
          write << e << e.backtrace
        end
        Process.exit!
      }
      write.close
      read2.close
      pgid_child = read.gets
      read.close
      pgid = Process.getsid(pid)
      write2.close
      Process.wait pid

      pgid_child = Integer(pgid_child)
      pgid_child.should == pgid
      pgid_child.should_not == Process.getsid
    end
  end
end
