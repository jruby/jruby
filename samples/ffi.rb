require 'ffi'

module POSIX
  extend FFI::Library
  # this line isn't really necessary since libc is always linked into JVM
  ffi_lib 'c'
  
  attach_function :getuid, :getuid, [], :uid_t
  attach_function :getpid, :getpid, [], :pid_t
end

puts "Process #{POSIX.getpid} running as user #{POSIX.getuid}"