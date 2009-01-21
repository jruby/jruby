require 'benchmark'
require 'ffi'


include Java
import java.nio.ByteBuffer

iter = 10_000

module Posix
  extend FFI::Library
  if FFI::Platform.linux?
    attach_function :__xstat, :__xstat64, [ :int, :string, :buffer_out ], :int
    STAT_VER = FFI::Platform::ADDRESS_SIZE == 32 ? 3 : 0
    def self.stat(path, ptr)
      __xstat(STAT_VER, path, ptr)
    end
  else
    attach_function :stat, [ :string, :buffer_out ], :int
  end
end
begin
  require File.join(FFI::Platform::CONF_DIR, 'stat.rb')
  Stat = Platform::Stat::Stat
rescue Exception => ex
  puts "Failed to load stat.rb: #{ex}"
  class Stat < FFI::Struct
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
end
module FFIFile
  def self.stat(file_name)
    st = Stat.new_out
    return st if Posix.stat(file_name, st) == 0
    raise "No such file or directory - #{file_name}"
  end
  def self.mtime(file_name)
    return Time.at(stat(file_name)[:st_mtime])
  end
  def self.ctime(file_name)
    return Time.at(stat(file_name)[:st_ctime])
  end
end

puts "Stat.size=#{Stat.size}"
st = Stat.alloc_out
Posix.stat("/tmp", st)
puts "mtime=#{st[:st_mtime]} File.stat.mtime=#{File.stat('/tmp').mtime.to_i}"
puts "FFIFile.mtime=#{FFIFile.mtime('/tmp')} File.stat.mtime=#{File.stat('/tmp').mtime}"
puts "FFIFile.ctime=#{FFIFile.ctime('/tmp')} File.stat.ctime=#{File.stat('/tmp').ctime}"
puts "size=#{st[:st_size]} File.stat.size=#{File.stat('/tmp').size.to_i}"
puts "blocks=#{st[:st_blocks]} File.stat.blocks=#{File.stat('/tmp').blocks.to_i}"
puts "FFI stat(file) #{iter}x"
10.times {
  puts Benchmark.measure {

    iter.times do
      # Allocate on the java/ruby heap, data only copied out of native memory, not in to it
      buf = Stat.alloc_out false # don't clear the memory
      Posix.stat("/tmp", buf)
    end
  }
}
puts "FFI stat(file) with Ruby Struct wrapping #{iter}x"
StatStruct = Struct.new(:st_ino, :st_mtime, :st_ctime, :st_atime, :st_blocks)
10.times {
  puts Benchmark.measure {

    iter.times do
      buf = Stat.alloc_out false # don't clear the memory
      Posix.stat("/tmp", buf)
      StatStruct.new(buf[:st_ino], Time.at(buf[:st_mtime]), Time.at(buf[:st_ctime]), Time.at(buf[:st_atime]),
        buf[:st_blocks])
    end
  }
}
puts "File.stat(file) #{iter}x"
10.times {
  puts Benchmark.measure {

    iter.times do
      File.stat("/tmp")
    end
  }
}
puts "File.mtime(file) #{iter}x"
10.times {
  puts Benchmark.measure {
    iter.times do
      File.mtime("/tmp")
    end
  }
}
puts "FFIFile.mtime(file) #{iter}x"
10.times {
  puts Benchmark.measure {
    iter.times do
      FFIFile.mtime("/tmp")
    end
  }
}

puts "File.ctime(file) #{iter}x"
10.times {
  puts Benchmark.measure {
    iter.times do
      File.ctime("/tmp")
    end
  }
}
puts "FFIFile.ctime(file) #{iter}x"
10.times {
  puts Benchmark.measure {
    iter.times do
      FFIFile.ctime("/tmp")
    end
  }
}
