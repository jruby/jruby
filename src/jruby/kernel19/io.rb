# Under MRI, EAGAIN is extended on every creation with the appropriate module.
# Due to the absurd overhead that results, we use these classes instead.
class IO
  class EAGAINWaitReadable < Errno::EAGAIN
    include IO::WaitReadable
  end
  
  class EAGAINWaitWritable < Errno::EAGAIN
    include IO::WaitWritable
  end

  class EINPROGRESSWaitWritable < Errno::EINPROGRESS
    include IO::WaitWritable
  end
end