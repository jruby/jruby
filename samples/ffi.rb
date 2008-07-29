require 'ffi'

module POSIX
  # this line isn't really necessary since libc is always linked into JVM
  set_ffi_lib 'c'
  
  attach_function :getuid, :getuid, [], :uint
  attach_function :getpid, :getpid, [], :uint
end

puts "Process #{POSIX.getpid} running as user #{POSIX.getuid}"