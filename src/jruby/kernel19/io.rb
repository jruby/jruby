# Under MRI, EAGAIN is extended on every creation with the appropriate module.
# Due to the absurd overhead that results, we use these classes instead.
module JRuby
  class EAGAINReadable < Errno::EAGAIN
    include IO::WaitReadable
  end
  
  class EAGAINWritable < Errno::EAGAIN
    include IO::WaitWritable
  end

  class EINPROGRESSWritable < Errno::EINPROGRESS
    include IO::WaitWritable
  end
end