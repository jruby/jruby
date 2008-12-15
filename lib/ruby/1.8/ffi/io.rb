module FFI
  module IO
    def self.for_fd(fd, mode)
      FFI::FileDescriptorIO.new(fd)
    end
  end
end