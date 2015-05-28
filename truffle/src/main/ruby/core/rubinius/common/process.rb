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
  module Constants
    EXIT_SUCCESS = Rubinius::Config['rbx.platform.process.EXIT_SUCCESS'] || 0
    EXIT_FAILURE = Rubinius::Config['rbx.platform.process.EXIT_FAILURE'] || 1

    PRIO_PGRP    = Rubinius::Config['rbx.platform.process.PRIO_PGRP']
    PRIO_PROCESS = Rubinius::Config['rbx.platform.process.PRIO_PROCESS']
    PRIO_USER    = Rubinius::Config['rbx.platform.process.PRIO_USER']

    RLIM_INFINITY  = Rubinius::Config['rbx.platform.process.RLIM_INFINITY']
    RLIM_SAVED_MAX = Rubinius::Config['rbx.platform.process.RLIM_SAVED_MAX']
    RLIM_SAVED_CUR = Rubinius::Config['rbx.platform.process.RLIM_SAVED_CUR']

    RLIMIT_AS      = Rubinius::Config['rbx.platform.process.RLIMIT_AS']
    RLIMIT_CORE    = Rubinius::Config['rbx.platform.process.RLIMIT_CORE']
    RLIMIT_CPU     = Rubinius::Config['rbx.platform.process.RLIMIT_CPU']
    RLIMIT_DATA    = Rubinius::Config['rbx.platform.process.RLIMIT_DATA']
    RLIMIT_FSIZE   = Rubinius::Config['rbx.platform.process.RLIMIT_FSIZE']
    RLIMIT_MEMLOCK = Rubinius::Config['rbx.platform.process.RLIMIT_MEMLOCK']
    RLIMIT_NOFILE  = Rubinius::Config['rbx.platform.process.RLIMIT_NOFILE']
    RLIMIT_NPROC   = Rubinius::Config['rbx.platform.process.RLIMIT_NPROC']
    RLIMIT_RSS     = Rubinius::Config['rbx.platform.process.RLIMIT_RSS']
    RLIMIT_SBSIZE  = Rubinius::Config['rbx.platform.process.RLIMIT_SBSIZE']
    RLIMIT_STACK   = Rubinius::Config['rbx.platform.process.RLIMIT_STACK']

    RLIMIT_RTPRIO     = Rubinius::Config['rbx.platform.process.RLIMIT_RTPRIO']
    RLIMIT_RTTIME     = Rubinius::Config['rbx.platform.process.RLIMIT_RTTIME']
    RLIMIT_SIGPENDING = Rubinius::Config['rbx.platform.process.RLIMIT_SIGPENDING']
    RLIMIT_MSGQUEUE   = Rubinius::Config['rbx.platform.process.RLIMIT_MSGQUEUE']
    RLIMIT_NICE       = Rubinius::Config['rbx.platform.process.RLIMIT_NICE']

    WNOHANG = 1
    WUNTRACED = 2
  end
  include Constants

  FFI = Rubinius::FFI

  def self.times
    Struct::Tms.new(*cpu_times)
  end

  def self.getpgid(pid)
    pid = Rubinius::Type.coerce_to pid, Integer, :to_int

    ret = FFI::Platform::POSIX.getpgid(pid)
    Errno.handle if ret == -1
    ret
  end

  @maxgroups = 32
  class << self
    attr_reader :maxgroups
    def maxgroups=(m)
      @maxgroups = m
    end
  end

  def self.getpgrp
    ret = FFI::Platform::POSIX.getpgrp
    Errno.handle if ret == -1
    ret
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

  def self.getpriority(kind, id)
    kind = Rubinius::Type.coerce_to kind, Integer, :to_int
    id =   Rubinius::Type.coerce_to id, Integer, :to_int

    FFI::Platform::POSIX.errno = 0
    ret = FFI::Platform::POSIX.getpriority(kind, id)
    Errno.handle
    ret
  end

  def self.setpriority(kind, id, priority)
    kind = Rubinius::Type.coerce_to kind, Integer, :to_int
    id =   Rubinius::Type.coerce_to id, Integer, :to_int
    priority = Rubinius::Type.coerce_to priority, Integer, :to_int

    ret = FFI::Platform::POSIX.setpriority(kind, id, priority)
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

  #
  # Wait for all child processes.
  #
  # Blocks until all child processes have exited, and returns
  # an Array of [pid, Process::Status] results, one for each
  # child.
  #
  # Be mindful of the effects of creating new processes while
  # .waitall has been called (usually in a different thread.)
  # The .waitall call does not in any way check that it is only
  # waiting for children that existed at the time it was called.
  #
  def self.waitall
    statuses = []

    begin
      while true
        statuses << Process.wait2
      end
    rescue Errno::ECHILD
    end

    statuses
  end

  #
  # Wait for the given process to exit.
  #
  # The pid may be the specific pid of some previously forked
  # process, or -1 to indicate to watch for *any* child process
  # exiting. Other options, such as process groups, may be available
  # depending on the system.
  #
  # With no arguments the default is to block waiting for any
  # child processes (pid -1.)
  #
  # The flag may be Process::WNOHANG, which indicates that
  # the child should only be quickly checked. If it has not
  # exited yet, nil is returned immediately instead.
  #
  # The return value is the exited pid or nil if Process::WNOHANG
  # was used and the child had not yet exited.
  #
  # If the pid has exited, the global $? is set to a Process::Status
  # object representing the exit status (and possibly other info) of
  # the child.
  #
  # If there exists no such pid (e.g. never forked or already
  # waited for), or no children at all, Errno::ECHILD is raised.
  #
  # TODO: Support other options such as WUNTRACED? --rue
  #
  def self.wait2(input_pid=-1, flags=nil)
    input_pid = Rubinius::Type.coerce_to input_pid, Integer, :to_int

    if flags and (flags & WNOHANG) == WNOHANG
      value = wait_pid_prim input_pid, true
      return if value.nil?
    else
      value = wait_pid_prim input_pid, false
    end

    if value == false
      raise Errno::ECHILD, "No child process: #{input_pid}"
    end

    # wait_pid_prim returns a tuple when wait needs to communicate
    # the pid that was actually detected as stopped (since wait
    # can wait for all child pids, groups, etc)
    status, termsig, stopsig, pid = value

    status = Process::Status.new(pid, status, termsig, stopsig)
    Rubinius::Mirror::Process.set_status_global status

    [pid, status]
  end
  
  #--
  # TODO: Most of the fields aren't implemented yet.
  # TODO: Also, these objects should only need to be constructed by
  # Process.wait and family.
  #++

  class Status

    attr_reader :exitstatus
    attr_reader :termsig
    attr_reader :stopsig

    def initialize(pid, exitstatus, termsig=nil, stopsig=nil)
      @pid = pid
      @exitstatus = exitstatus
      @termsig = termsig
      @stopsig = stopsig
    end

    def to_i
      @exitstatus
    end

    def to_s
      @exitstatus.to_s
    end

    def &(num)
      @exitstatus & num
    end

    def ==(other)
      other = other.to_i if other.kind_of? Process::Status
      @exitstatus == other
    end

    def >>(num)
      @exitstatus >> num
    end

    def coredump?
      false
    end

    def exited?
      @exitstatus != nil
    end

    def pid
      @pid
    end

    def signaled?
      @termsig != nil
    end

    def stopped?
      @stopsig != nil
    end

    def success?
      if exited?
        @exitstatus == 0
      else
        nil
      end
    end
  end

  module Sys
    class << self
      def getegid
        ret = FFI::Platform::POSIX.getegid
        Errno.handle if ret == -1
        ret
      end

      def geteuid
        ret = FFI::Platform::POSIX.geteuid
        Errno.handle if ret == -1
        ret
      end

      def getgid
        ret = FFI::Platform::POSIX.getgid
        Errno.handle if ret == -1
        ret
      end

      def getuid
        ret = FFI::Platform::POSIX.getuid
        Errno.handle if ret == -1
        ret
      end

      def issetugid
        raise "not implemented"
      end

      def setgid(gid)
        gid = Rubinius::Type.coerce_to gid, Integer, :to_int

        ret = FFI::Platform::POSIX.setgid gid
        Errno.handle if ret == -1
        nil
      end

      def setuid(uid)
        uid = Rubinius::Type.coerce_to uid, Integer, :to_int

        ret = FFI::Platform::POSIX.setuid uid
        Errno.handle if ret == -1
        nil
      end

      def setegid(egid)
        egid = Rubinius::Type.coerce_to egid, Integer, :to_int

        ret = FFI::Platform::POSIX.setegid egid
        Errno.handle if ret == -1
        nil
      end

      def seteuid(euid)
        euid = Rubinius::Type.coerce_to euid, Integer, :to_int

        ret = FFI::Platform::POSIX.seteuid euid
        Errno.handle if ret == -1
        nil
      end

      def setrgid(rgid)
        setregid(rgid, -1)
      end

      def setruid(ruid)
        setreuid(ruid, -1)
      end

      def setregid(rid, eid)
        rid = Rubinius::Type.coerce_to rid, Integer, :to_int
        eid = Rubinius::Type.coerce_to eid, Integer, :to_int

        ret = FFI::Platform::POSIX.setregid rid, eid
        Errno.handle if ret == -1
        nil
      end

      def setreuid(rid, eid)
        rid = Rubinius::Type.coerce_to rid, Integer, :to_int
        eid = Rubinius::Type.coerce_to eid, Integer, :to_int

        ret = FFI::Platform::POSIX.setreuid rid, eid
        Errno.handle if ret == -1
        nil
      end

      def setresgid(rid, eid, sid)
        rid = Rubinius::Type.coerce_to rid, Integer, :to_int
        eid = Rubinius::Type.coerce_to eid, Integer, :to_int
        sid = Rubinius::Type.coerce_to sid, Integer, :to_int

        ret = FFI::Platform::POSIX.setresgid rid, eid, sid
        Errno.handle if ret == -1
        nil
      end

      def setresuid(rid, eid, sid)
        rid = Rubinius::Type.coerce_to rid, Integer, :to_int
        eid = Rubinius::Type.coerce_to eid, Integer, :to_int
        sid = Rubinius::Type.coerce_to sid, Integer, :to_int

        ret = FFI::Platform::POSIX.setresuid rid, eid, sid
        Errno.handle if ret == -1
        nil
      end
    end
  end

  module UID
    class << self
      def change_privilege(uid)
        uid = Rubinius::Type.coerce_to uid, Integer, :to_int

        ret = FFI::Platform::POSIX.setreuid(uid, uid)
        Errno.handle if ret == -1
        uid
      end

      def eid
        ret = FFI::Platform::POSIX.geteuid
        Errno.handle if ret == -1
        ret
      end

      def eid=(uid)
        uid = Rubinius::Type.coerce_to uid, Integer, :to_int

        ret = FFI::Platform::POSIX.seteuid(uid)
        Errno.handle if ret == -1
        uid
      end
      alias_method :grant_privilege, :eid=

      def re_exchange
        real = FFI::Platform::POSIX.getuid
        Errno.handle if real == -1
        eff = FFI::Platform::POSIX.geteuid
        Errno.handle if eff == -1
        ret = FFI::Platform::POSIX.setreuid(eff, real)
        Errno.handle if ret == -1
        eff
      end

      def re_exchangeable?
        true
      end

      def rid
        ret = FFI::Platform::POSIX.getuid
        Errno.handle if ret == -1
        ret
      end

      def sid_available?
        true
      end

      def switch
        eff = re_exchange
        if block_given?
          ret = yield
          re_exchange
          return ret
        else
          return eff
        end
      end

    end
  end

  module GID
    class << self
      def change_privilege(gid)
        gid = Rubinius::Type.coerce_to gid, Integer, :to_int

        ret = FFI::Platform::POSIX.setregid(gid, gid)
        Errno.handle if ret == -1
        gid
      end

      def eid
        ret = FFI::Platform::POSIX.getegid
        Errno.handle if ret == -1
        ret
      end

      def eid=(gid)
        gid = Rubinius::Type.coerce_to gid, Integer, :to_int

        ret = FFI::Platform::POSIX.setegid(gid)
        Errno.handle if ret == -1
        gid
      end
      alias_method :grant_privilege, :eid=

      def re_exchange
        real = FFI::Platform::POSIX.getgid
        Errno.handle if real == -1
        eff = FFI::Platform::POSIX.getegid
        Errno.handle if eff == -1
        ret = FFI::Platform::POSIX.setregid(eff, real)
        Errno.handle if ret == -1
        eff
      end

      def re_exchangeable?
        true
      end

      def rid
        ret = FFI::Platform::POSIX.getgid
        Errno.handle if ret == -1
        ret
      end

      def sid_available?
        true
      end

      def switch
        eff = re_exchange
        if block_given?
          ret = yield
          re_exchange
          return ret
        else
          return eff
        end
      end

    end
  end

end
