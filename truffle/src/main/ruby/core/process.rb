# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# Copyright (c) 2007-2015, Evan Phoenix and contributors
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

    has_rlimit_rtprio = Rubinius::Config['rbx.platform.process.RLIMIT_RTPRIO']
    if has_rlimit_rtprio
      RLIMIT_RTPRIO     = Rubinius::Config['rbx.platform.process.RLIMIT_RTPRIO']
      RLIMIT_RTTIME     = Rubinius::Config['rbx.platform.process.RLIMIT_RTTIME']
      RLIMIT_SIGPENDING = Rubinius::Config['rbx.platform.process.RLIMIT_SIGPENDING']
      RLIMIT_MSGQUEUE   = Rubinius::Config['rbx.platform.process.RLIMIT_MSGQUEUE']
      RLIMIT_NICE       = Rubinius::Config['rbx.platform.process.RLIMIT_NICE']
    end

    WNOHANG = 1
    WUNTRACED = 2
  end
  include Constants

  FFI = Rubinius::FFI

  class Rlimit < FFI::Struct
    config "rbx.platform.rlimit", :rlim_cur, :rlim_max
  end

  # Terminate with given status code.
  #
  def self.exit(code=0)
    case code
      when true
        code = 0
      when false
        code = 1
      else
        code = Rubinius::Type.coerce_to code, Integer, :to_int
    end

    raise SystemExit.new(code)
  end

  def self.exit!(code=1)
    Truffle.primitive :vm_exit

    case code
      when true
        exit! 0
      when false
        exit! 1
      else
        exit! Rubinius::Type.coerce_to(code, Integer, :to_int)
    end
  end

  def self.wait_pid_prim(pid, no_hang)
    Truffle.primitive :vm_wait_pid
    raise PrimitiveFailure, "Process.wait_pid primitive failed"
  end

  def self.time
    Truffle.primitive :vm_time
    raise PrimitiveFailure, "Process.time primitive failed"
  end

  def self.cpu_times
    Truffle.primitive :vm_times
    raise PrimitiveFailure, "Process.cpu_times primitive failed"
  end

  ##
  # Sets the process title. Calling this method does not affect the value of
  # `$0` as per MRI behaviour. This method returns the title set.
  #
  # @param [String] title
  # @return [Title]
  #
  def self.setproctitle(title)
    val = Rubinius::Type.coerce_to(title, String, :to_str)
    Truffle.invoke_primitive(:vm_set_process_title, val)
  end

  def self.setrlimit(resource, cur_limit, max_limit=undefined)
    resource =  coerce_rlimit_resource(resource)
    cur_limit = Rubinius::Type.coerce_to cur_limit, Integer, :to_int

    unless undefined.equal? max_limit
      max_limit = Rubinius::Type.coerce_to max_limit, Integer, :to_int
    end

    rlimit = Rlimit.new
    rlimit[:rlim_cur] = cur_limit
    rlimit[:rlim_max] = undefined.equal?(max_limit) ? cur_limit : max_limit

    ret = Truffle::POSIX.setrlimit(resource, rlimit.pointer)
    Errno.handle if ret == -1
    nil
  end

  def self.getrlimit(resource)
    resource = coerce_rlimit_resource(resource)

    lim_max = []
    rlimit = Rlimit.new
    ret = Truffle::POSIX.getrlimit(resource, rlimit.pointer)
    Errno.handle if ret == -1

    [rlimit[:rlim_cur], rlimit[:rlim_max]]
  end

  def self.setsid
    pgid = Truffle::POSIX.setsid
    Errno.handle if pgid == -1
    pgid
  end

  def self.fork
    raise 'unsupported'
  end

  def self.times
    Struct::Tms.new(*cpu_times)
  end

  def self.kill(signal, *pids)
    raise ArgumentError, "PID argument required" if pids.length == 0

    use_process_group = false
    signal = signal.to_s if signal.kind_of?(Symbol)

    if signal.kind_of?(String)
      if signal[0] == ?-
        signal = signal[1..-1]
        use_process_group = true
      end

      if signal[0..2] == "SIG"
        signal = signal[3..-1]
      end

      signal = Signal::Names[signal]
    end

    raise ArgumentError unless signal.kind_of? Fixnum

    if signal < 0
      signal = -signal
      use_process_group = true
    end

    signal_name = Signal::Numbers[signal]

    pids.each do |pid|
      pid = Rubinius::Type.coerce_to pid, Integer, :to_int

      pid = -pid if use_process_group
      result = Truffle::POSIX.kill(pid, signal, signal_name)

      Errno.handle if result == -1
    end

    pids.length
  end

  def self.abort(msg=nil)
    if msg
      msg = StringValue(msg)
      $stderr.puts(msg)
    end
    raise SystemExit.new(1, msg)
  end

  def self.getpgid(pid)
    pid = Rubinius::Type.coerce_to pid, Integer, :to_int

    ret = Truffle::POSIX.getpgid(pid)
    Errno.handle if ret == -1
    ret
  end

  def self.setpgid(pid, int)
    pid = Rubinius::Type.coerce_to pid, Integer, :to_int
    int = Rubinius::Type.coerce_to int, Integer, :to_int

    ret = Truffle::POSIX.setpgid(pid, int)
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

  def self.setpgrp
    setpgid(0, 0)
  end
  def self.getpgrp
    ret = Truffle::POSIX.getpgrp
    Errno.handle if ret == -1
    ret
  end

  def self.ppid
    ret = Truffle::POSIX.getppid
    Errno.handle if ret == -1
    ret
  end

  def self.uid=(uid)
    # the 4 rescue clauses below are needed
    # until respond_to? can be used to query the implementation of methods attached via FFI
    # atm respond_to returns true if a method is attached but not implemented on the platform
    uid = Rubinius::Type.coerce_to uid, Integer, :to_int
    begin
      ret = Truffle::POSIX.setresuid(uid, -1, -1)
    rescue NotImplementedError
      begin
        ret = Truffle::POSIX.setreuid(uid, -1)
      rescue NotImplementedError
        begin
          ret = Truffle::POSIX.setruid(uid)
        rescue NotImplementedError
          if Process.euid == uid
            ret = Truffle::POSIX.setuid(uid)
          else
            raise NotImplementedError
          end
        end
      end
    end

    Errno.handle if ret == -1

    uid
  end

  def self.gid=(gid)
    gid = Rubinius::Type.coerce_to gid, Integer, :to_int
    Process::Sys.setgid gid
  end

  def self.euid=(uid)
    # the 4 rescue clauses below are needed
    # until respond_to? can be used to query the implementation of methods attached via FFI
    # atm respond_to returns true if a method is attached but not implemented on the platform
    uid = Rubinius::Type.coerce_to uid, Integer, :to_int
    begin
      ret = Truffle::POSIX.setresuid(-1, uid, -1)
    rescue NotImplementedError
      begin
        ret = Truffle::POSIX.setreuid(-1, uid)
      rescue NotImplementedError
        begin
          ret = Truffle::POSIX.seteuid(uid)
        rescue NotImplementedError
          if Process.uid == uid
            ret = Truffle::POSIX.setuid(uid)
          else
            raise NotImplementedError
          end
        end
      end
    end

    Errno.handle if ret == -1

    uid
  end

  def self.egid=(gid)
    gid = Rubinius::Type.coerce_to gid, Integer, :to_int
    Process::Sys.setegid gid
  end

  def self.uid
    ret = Truffle::POSIX.getuid
    Errno.handle if ret == -1
    ret
  end

  def self.gid
    ret = Truffle::POSIX.getgid
    Errno.handle if ret == -1
    ret
  end

  def self.euid
    ret = Truffle::POSIX.geteuid
    Errno.handle if ret == -1
    ret
  end

  def self.egid
    ret = Truffle::POSIX.getegid
    Errno.handle if ret == -1
    ret
  end

  def self.getpriority(kind, id)
    kind = Rubinius::Type.coerce_to kind, Integer, :to_int
    id =   Rubinius::Type.coerce_to id, Integer, :to_int

    ret = Truffle::POSIX.getpriority(kind, id)
    Errno.handle
    ret
  end

  def self.setpriority(kind, id, priority)
    kind = Rubinius::Type.coerce_to kind, Integer, :to_int
    id =   Rubinius::Type.coerce_to id, Integer, :to_int
    priority = Rubinius::Type.coerce_to priority, Integer, :to_int

    ret = Truffle::POSIX.setpriority(kind, id, priority)
    Errno.handle if ret == -1
    ret
  end

  def self.groups
    g = []
    count = Truffle::POSIX.getgroups(0, nil)
    FFI::MemoryPointer.new(:int, count) { |p|
      num_groups = Truffle::POSIX.getgroups(count, p)
      Errno.handle if num_groups == -1
      g = p.read_array_of_int(num_groups)
    }
    g
  end

  def self.groups=(g)
    @maxgroups = g.length if g.length > @maxgroups
    FFI::MemoryPointer.new(:int, @maxgroups) { |p|
      p.write_array_of_int(g)
      Errno.handle if Truffle::POSIX.setgroups(g.length, p) == -1
    }
    g
  end

  def self.initgroups(username, gid)
    username = StringValue(username)
    gid = Rubinius::Type.coerce_to gid, Integer, :to_int

    if Truffle::POSIX.initgroups(username, gid) == -1
      Errno.handle
    end

    Process.groups
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

  def self.wait(pid=-1, flags=nil)
    pid, status = Process.wait2(pid, flags)
    return pid
  end

  class << self
    alias_method :waitpid, :wait
    alias_method :waitpid2, :wait2
  end

  Rubinius::Globals.read_only :$?
  Rubinius::Globals.set_hook(:$?) { Thread.current[:$?] }

  def self.daemon(stay_in_dir=false, keep_stdio_open=false)
    # Do not run at_exit handlers in the parent
    exit!(0) if fork

    Process.setsid

    exit!(0) if fork

    Dir.chdir("/") unless stay_in_dir

    unless keep_stdio_open
      io = File.open "/dev/null", File::RDWR, 0
      $stdin.reopen io
      $stdout.reopen io
      $stderr.reopen io
    end

    return 0
  end

  def self.exec(*args)
    Rubinius::Mirror::Process.exec(*args)
  end

  def self.spawn(*args)
    Rubinius::Mirror::Process.spawn(*args)
  end

  # TODO: Should an error be raised on ECHILD? --rue
  #
  # TODO: This operates on the assumption that waiting on
  #       the event consumes very little resources. If this
  #       is not the case, the check should be made WNOHANG
  #       and called periodically.
  #
  def self.detach(pid)
    raise ArgumentError, "Only positive pids may be detached" unless pid > 0

    thread = Thread.new { Process.wait pid; $? }
    thread[:pid] = pid
    def thread.pid; self[:pid] end

    thread
  end

  def self.coerce_rlimit_resource(resource)
    case resource
    when Integer
      return resource
    when Symbol, String
      # do nothing
    else
      unless r = Rubinius::Type.check_convert_type(resource, String, :to_str)
        return Rubinius::Type.coerce_to resource, Integer, :to_int
      end

      resource = r
    end

    constant = "RLIMIT_#{resource}"
    unless const_defined? constant
      raise ArgumentError, "invalid resource name: #{constant}"
    end
    const_get constant
  end
  private_class_method :coerce_rlimit_resource

  #--
  # TODO: Most of the fields aren't implemented yet.
  # TODO: Also, these objects should only need to be constructed by
  # Process.wait and family.
  #++

  class Status

    attr_reader :termsig
    attr_reader :stopsig

    def initialize(pid=nil, status=nil, termsig=nil, stopsig=nil)
      @pid = pid
      @status = status
      @termsig = termsig
      @stopsig = stopsig
    end

    def exitstatus
      @status
    end

    def to_i
      @status
    end

    def to_s
      @status.to_s
    end

    def &(num)
      @status & num
    end

    def ==(other)
      other = other.to_i if other.kind_of? Process::Status
      @status == other
    end

    def >>(num)
      @status >> num
    end

    def coredump?
      false
    end

    def exited?
      @status != nil
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
        @status == 0
      else
        nil
      end
    end
  end

  module Sys
    class << self
      def getegid
        ret = Truffle::POSIX.getegid
        Errno.handle if ret == -1
        ret
      end

      def geteuid
        ret = Truffle::POSIX.geteuid
        Errno.handle if ret == -1
        ret
      end

      def getgid
        ret = Truffle::POSIX.getgid
        Errno.handle if ret == -1
        ret
      end

      def getuid
        ret = Truffle::POSIX.getuid
        Errno.handle if ret == -1
        ret
      end

      def issetugid
        raise "not implemented"
      end

      def setgid(gid)
        gid = Rubinius::Type.coerce_to gid, Integer, :to_int

        ret = Truffle::POSIX.setgid gid
        Errno.handle if ret == -1
        nil
      end

      def setuid(uid)
        uid = Rubinius::Type.coerce_to uid, Integer, :to_int

        ret = Truffle::POSIX.setuid uid
        Errno.handle if ret == -1
        nil
      end

      def setegid(egid)
        egid = Rubinius::Type.coerce_to egid, Integer, :to_int

        ret = Truffle::POSIX.setegid egid
        Errno.handle if ret == -1
        nil
      end

      def seteuid(euid)
        euid = Rubinius::Type.coerce_to euid, Integer, :to_int

        ret = Truffle::POSIX.seteuid euid
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

        ret = Truffle::POSIX.setregid rid, eid
        Errno.handle if ret == -1
        nil
      end

      def setreuid(rid, eid)
        rid = Rubinius::Type.coerce_to rid, Integer, :to_int
        eid = Rubinius::Type.coerce_to eid, Integer, :to_int

        ret = Truffle::POSIX.setreuid rid, eid
        Errno.handle if ret == -1
        nil
      end

      def setresgid(rid, eid, sid)
        rid = Rubinius::Type.coerce_to rid, Integer, :to_int
        eid = Rubinius::Type.coerce_to eid, Integer, :to_int
        sid = Rubinius::Type.coerce_to sid, Integer, :to_int

        ret = Truffle::POSIX.setresgid rid, eid, sid
        Errno.handle if ret == -1
        nil
      end

      def setresuid(rid, eid, sid)
        rid = Rubinius::Type.coerce_to rid, Integer, :to_int
        eid = Rubinius::Type.coerce_to eid, Integer, :to_int
        sid = Rubinius::Type.coerce_to sid, Integer, :to_int

        ret = Truffle::POSIX.setresuid rid, eid, sid
        Errno.handle if ret == -1
        nil
      end
    end
  end

  module UID
    class << self
      def change_privilege(uid)
        uid = Rubinius::Type.coerce_to uid, Integer, :to_int

        ret = Truffle::POSIX.setreuid(uid, uid)
        Errno.handle if ret == -1
        uid
      end

      def eid
        ret = Truffle::POSIX.geteuid
        Errno.handle if ret == -1
        ret
      end

      def eid=(uid)
        uid = Rubinius::Type.coerce_to uid, Integer, :to_int

        ret = Truffle::POSIX.seteuid(uid)
        Errno.handle if ret == -1
        uid
      end
      alias_method :grant_privilege, :eid=

      def re_exchange
        real = Truffle::POSIX.getuid
        Errno.handle if real == -1
        eff = Truffle::POSIX.geteuid
        Errno.handle if eff == -1
        ret = Truffle::POSIX.setreuid(eff, real)
        Errno.handle if ret == -1
        eff
      end

      def re_exchangeable?
        true
      end

      def rid
        ret = Truffle::POSIX.getuid
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

        ret = Truffle::POSIX.setregid(gid, gid)
        Errno.handle if ret == -1
        gid
      end

      def eid
        ret = Truffle::POSIX.getegid
        Errno.handle if ret == -1
        ret
      end

      def eid=(gid)
        gid = Rubinius::Type.coerce_to gid, Integer, :to_int

        ret = Truffle::POSIX.setegid(gid)
        Errno.handle if ret == -1
        gid
      end
      alias_method :grant_privilege, :eid=

      def re_exchange
        real = Truffle::POSIX.getgid
        Errno.handle if real == -1
        eff = Truffle::POSIX.getegid
        Errno.handle if eff == -1
        ret = Truffle::POSIX.setregid(eff, real)
        Errno.handle if ret == -1
        eff
      end

      def re_exchangeable?
        true
      end

      def rid
        ret = Truffle::POSIX.getgid
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

$$ = Process.pid
