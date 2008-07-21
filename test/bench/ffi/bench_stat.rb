# 
# To change this template, choose Tools | Templates
# and open the template in the editor.
 
require 'benchmark'
require 'ffi'

include Java
import java.nio.ByteBuffer

module Foo
  jffi_attach :int, :stat, [ :string, :buffer_out ]
end
class Stat < JFFI::Struct
  layout \
    :st_dev => :int,        # device inode resides on (dev_t)
    :st_ino => :int,        # inode's number (ino_t)
    :st_mode => :uint16,    # inode protection mode (mode_t - uint16)
    :st_nlink => :uint16,   # number or hard links to the file (nlink_y - uint16)
    :st_uid => :int,        # user-id of owner (uid_t)
    :st_gid => :int,        # group-id of owner (gid_t)
    :st_rdev => :int,       # device type, for special file inode (st_rdev - dev_t)
    :st_atime => :long,     # Time of last access (time_t)
    :st_atimensec => :long, # Time of last access (nanoseconds)
    :st_mtime => :long,     # Last data modification time (time_t)
    :st_mtimensec => :long, # Last data modification time (nanoseconds)
    :st_ctime => :long,     # Time of last status change (time_t)
    :st_ctimensec => :long, # Time of last status change (nanoseconds)
    :st_size => :uint64,    # file size, in bytes
    :st_blocks => :uint64,  # blocks allocated for file
    :st_blksize => :int,    # optimal file system I/O ops blocksize
    :st_flags => :int,      # user defined flags for file
    :st_gen => :int,        # file generation number
    :st_lspare => :int,     # RESERVED: DO NOT USE!
    :st_qspare0 => :long_long, # RESERVED: DO NOT USE!
    :st_qspare1 => :long_long # RESERVED: DO NOT USE!
  
end
puts "Stat.size=#{Stat.size}"
st = Stat.new
Foo.stat("/tmp", st.pointer)
puts "mtime=#{st[:st_mtime]} File.stat.mtime=#{File.stat('/tmp').mtime.to_i}"
puts "size=#{st[:st_size]} File.stat.size=#{File.stat('/tmp').size.to_i}"
puts "10k FFI stat(file)"
20.times {
  puts Benchmark.measure {

    10_000.times do
      buf = Stat.allocate # Allocate on the heap
      Foo.stat("/tmp", buf.pointer)
    end
  }
}
puts "10k File.stat(file)"
20.times {
  puts Benchmark.measure {

    10_000.times do
      File.stat("/tmp")
    end
  }
}

