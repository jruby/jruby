require 'benchmark'
require 'ffi'
require 'etc'

require File.join(JFFI::Platform::CONF_DIR, "etc")
Passwd = Platform::Etc::Passwd

iter = 100000

class Posix
  # jffi_attach passes the :string param in as a NUL terminated constant string
  jffi_attach :pointer, 'getpwnam', [ :string ], { :from => 'c', :as => 'jgetpwnam' }

  # attach_foreign passes the :string param in as a mutable NUL terminated string for
  # rubinius backward compatibility, but the copyback introduces some overhead
  attach_foreign :pointer, 'getpwnam', [ :string ], :from => 'c'
end

login = Etc.getlogin

ffiname = Passwd.new(Posix.getpwnam(login))[:pw_name]
etcname = Etc.getpwnam(login).name
puts "pw_name does not match Etc.getpwnam.name" if ffiname != etcname

pwd = Etc.getpwnam(login)
puts "members=#{pwd.members.inspect}"
puts "values=#{pwd.values.inspect}"

pwd = Passwd.new(Posix.getpwnam(login))
puts "members=#{pwd.members.inspect}"
puts "values=#{pwd.values.inspect}"


puts "Benchmark FFI getpwnam (rubinius api) performance, #{iter}x"

10.times {
  puts Benchmark.measure {
    iter.times {
      pwd = Passwd.new Posix.getpwnam(login)
      pwd[:pw_name]
      pwd[:pw_uid]
      pwd[:pw_gid]
    }
  }
}
puts "Benchmark FFI getpwnam (jruby api) performance, #{iter}x"

10.times {
  puts Benchmark.measure {
    iter.times {
      pwd = Passwd.new Posix.jgetpwnam(login)
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

