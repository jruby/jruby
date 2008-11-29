require 'benchmark'
require 'ffi'
require 'etc'

require File.join(FFI::Platform::CONF_DIR, "etc")
Passwd = Platform::Etc::Passwd

iter = 100000

module JPosix
  extend FFI::Library
  attach_function :getpwnam, [ :string ], :pointer
end

login = Etc.getlogin

ffiname = Passwd.new(JPosix.getpwnam(login))[:pw_name]
etcname = Etc.getpwnam(login).name
puts "pw_name does not match Etc.getpwnam.name" if ffiname != etcname

pwd = Etc.getpwnam(login)
puts "members=#{pwd.members.inspect}"
puts "values=#{pwd.values.inspect}"

pwd = Passwd.new(JPosix.getpwnam(login))
puts "members=#{pwd.members.inspect}"
puts "values=#{pwd.values.inspect}"


puts "Benchmark FFI getpwnam performance, #{iter}x"

10.times {
  puts Benchmark.measure {
    iter.times {
      pwd = Passwd.new JPosix.getpwnam(login)
      pwd[:pw_name]
      pwd[:pw_uid]
      pwd[:pw_gid]
    }
  }
}
puts "Benchmark Etc.getpwnam performance, #{iter}x"
10.times {
  puts Benchmark.measure {
    iter.times { 
      pwd = Etc.getpwnam(login); 
      
      pwd.name 
      pwd.uid
      pwd.gid
    }
  }
}

