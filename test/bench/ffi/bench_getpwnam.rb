# 
# To change this template, choose Tools | Templates
# and open the template in the editor.
require 'benchmark'
require 'ffi'
require 'etc'

class Passwd < JFFI::Struct
  layout \
    :pw_name => :string,   # user name
    :pw_passwd => :string, # encrypted password
    :pw_uid => :int,       # user uid
    :pw_gid => :int,       # user gid
    :pw_change => :long,   # password change time
    :pw_class => :string,  # user access class
    :pw_gecos => :string,  # Honeywell login info
    :pw_dir => :string,    # home directory
    :pw_shell => :string,  # default shell
    :pw_expire => :long    # account expiration
end
class TestPasswd
  jffi_attach :pointer, :getpwnam, [ :string ]
end

login = Etc.getlogin

ffiname = Passwd.new(TestPasswd.getpwnam(login))[:pw_name]
etcname = Etc.getpwnam(login).name
puts "pw_name does not match Etc.getpwnam.name" if ffiname != etcname

pwd = Etc.getpwnam(login)
puts "members=#{pwd.members.inspect}"
puts "values=#{pwd.values.inspect}"

pwd = Passwd.new(TestPasswd.getpwnam(login))
puts "members=#{pwd.members.inspect}"
puts "values=#{pwd.values.inspect}"

iter = 10000
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
puts "Benchmark FFI getpwnam performance, #{iter}x"

10.times {
  puts Benchmark.measure {
    iter.times { 
      pwd = Passwd.new TestPasswd.getpwnam(login)
      pwd[:pw_name] 
#      pwd[:pw_uid]
#      pwd[:pw_gid]
#      pwd[:pw_change]
#      pwd[:pw_expire]
    }
  }
}
