module Krypt::FFI

  module LibC
    extend ::FFI::Library

    ffi_lib ::FFI::Library::LIBC

    attach_function :malloc, [:size_t], :pointer
    attach_function :free, [:pointer], :void
    attach_function :memcpy, [:pointer, :pointer, :size_t], :void
  end

end
