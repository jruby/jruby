module FFI
  module IO
    def self.for_fd(fd, mode)
      FFI::FileDescriptorIO.new(fd)
    end

    #
    # A version of IO#read that reads into a native buffer
    # 
    # This will be optimized at some future time to eliminate the double copy
    #
    # def self.native_read(io, buf, len)

  end
end

