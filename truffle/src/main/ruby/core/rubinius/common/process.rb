# Copyright (c) 2007-2014, Evan Phoenix and contributors
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
# * Redistributions of source code must retain the above copyright notice, this
#   list of conditions and the following disclaimer.
# * Redistributions in binary form must reproduce the above copyright notice
#   this list of conditions and the following disclaimer in the documentation
#   and/or other materials provided with the distribution.
# * Neither the name of Rubinius nor the names of its contributors
#   may be used to endorse or promote products derived from this software
#   without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
# FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
# SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

# Only part of Rubinius' process.rb

module Process

  FFI = Rubinius::FFI

  def self.times
    Struct::Tms.new(*cpu_times)
  end

  @maxgroups = 32
  class << self
    attr_reader :maxgroups
    def maxgroups=(m)
      @maxgroups = m
    end
  end

  def self.uid
    ret = FFI::Platform::POSIX.getuid
    Errno.handle if ret == -1
    ret
  end

  def self.gid
    ret = FFI::Platform::POSIX.getgid
    Errno.handle if ret == -1
    ret
  end

  def self.euid
    ret = FFI::Platform::POSIX.geteuid
    Errno.handle if ret == -1
    ret
  end

  def self.egid
    ret = FFI::Platform::POSIX.getegid
    Errno.handle if ret == -1
    ret
  end

  def self.groups
    g = []
    FFI::MemoryPointer.new(:int, @maxgroups) { |p|
      num_groups = FFI::Platform::POSIX.getgroups(@maxgroups, p)
      Errno.handle if num_groups == -1
      g = p.read_array_of_int(num_groups)
    }
    g
  end

end
