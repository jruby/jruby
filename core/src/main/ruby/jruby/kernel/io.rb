class IO
  class EAGAINWaitReadable < Errno::EAGAIN
    include IO::WaitReadable
  end
  
  class EAGAINWaitWritable < Errno::EAGAIN
    include IO::WaitWritable
  end

  if Errno::EAGAIN == Errno::EWOULDBLOCK
    IO::EWOULDBLOCKWaitReadable = IO::EAGAINWaitReadable
    IO::EWOULDBLOCKWaitWritable = IO::EAGAINWaitWritable
  else
    class EWOULDBLOCKWaitReadable < Errno::EWOULDBLOCK
      include IO::WaitReadable
    end

    class EWOULDBLOCKWaitWritable < Errno::EWOULDBLOCK
      include IO::WaitWritable
    end
  end

  class EINPROGRESSWaitWritable < Errno::EINPROGRESS
    include IO::WaitWritable
  end

  # We provided this as an unofficial way to do "open4" on JRuby (since open4 gem depends on fork),
  # and unfortunately people started using it. So I think we're stuck with it now (at least until
  # we can fix the open4 gem to do what we do below).
  # FIXME: I don't think spawn works on Windows yet, but the old IO.popen4 did.
  # FIXME: Mostly copied from open3.rb impl of popen3.
  def self.popen4(*cmd, **opts)
    in_r, in_w = IO.pipe
    opts[:in] = in_r
    in_w.sync = true

    out_r, out_w = IO.pipe
    opts[:out] = out_w

    err_r, err_w = IO.pipe
    opts[:err] = err_w

    child_io = [in_r, out_w, err_w]
    parent_io = [in_w, out_r, err_r]

    pid = spawn(*cmd, opts)
    child_io.each {|io| io.close }
    result = [pid, *parent_io]
    if block_given?
      begin
        return yield(*result)
      ensure
        parent_io.each{|io| io.close unless io.closed?}
        Process.waitpid(pid)
      end
    end
    result
  end
end