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

module Rubinius
module Truffle::POSIX
  #--
  # Internal class for accessing timevals
  #++
  class TimeVal < FFI::Struct
    config 'rbx.platform.timeval', :tv_sec, :tv_usec
  end
end
end

class File < IO
  include Enumerable

  class FileError < Exception; end
  class NoFileError < FileError; end
  class UnableToStat < FileError; end
  class PermissionError < FileError; end

  # these will be necessary when we run on Windows
  DOSISH = false # !!(RUBY_PLATFORM =~ /mswin/)
  CASEFOLD_FILESYSTEM = DOSISH
  FNM_SYSCASE = CASEFOLD_FILESYSTEM ? FNM_CASEFOLD : 0

  module Constants
    F_GETFL  = Rubinius::Config['rbx.platform.fcntl.F_GETFL']
    F_SETFL  = Rubinius::Config['rbx.platform.fcntl.F_SETFL']

    # O_ACCMODE is /undocumented/ for fcntl() on some platforms
    ACCMODE  = Rubinius::Config['rbx.platform.fcntl.O_ACCMODE']

    F_GETFD  = Rubinius::Config['rbx.platform.fcntl.F_GETFD']
    F_SETFD  = Rubinius::Config['rbx.platform.fcntl.F_SETFD']
    FD_CLOEXEC = Rubinius::Config['rbx.platform.fcntl.FD_CLOEXEC']

    RDONLY   = Rubinius::Config['rbx.platform.file.O_RDONLY']
    WRONLY   = Rubinius::Config['rbx.platform.file.O_WRONLY']
    RDWR     = Rubinius::Config['rbx.platform.file.O_RDWR']

    CREAT    = Rubinius::Config['rbx.platform.file.O_CREAT']
    EXCL     = Rubinius::Config['rbx.platform.file.O_EXCL']
    NOCTTY   = Rubinius::Config['rbx.platform.file.O_NOCTTY']
    TRUNC    = Rubinius::Config['rbx.platform.file.O_TRUNC']
    APPEND   = Rubinius::Config['rbx.platform.file.O_APPEND']
    NONBLOCK = Rubinius::Config['rbx.platform.file.O_NONBLOCK']
    SYNC     = Rubinius::Config['rbx.platform.file.O_SYNC']

    if Rubinius::Config['rbx.platform.file.O_TMPFILE']
      TMPFILE = Rubinius::Config['rbx.platform.file.O_TMPFILE']
    end

    # TODO: these flags should probably be imported from Platform
    LOCK_SH  = 0x01
    LOCK_EX  = 0x02
    LOCK_NB  = 0x04
    LOCK_UN  = 0x08
    BINARY   = 0x04

    # TODO: "OK" constants aren't in File::Constants in MRI
    F_OK = 0 # test for existence of file
    X_OK = 1 # test for execute or search permission
    W_OK = 2 # test for write permission
    R_OK = 4 # test for read permission

    FNM_NOESCAPE = 0x01
    FNM_PATHNAME = 0x02
    FNM_DOTMATCH = 0x04
    FNM_CASEFOLD = 0x08
    FNM_EXTGLOB  = 0x10

    if Rubinius.windows?
      NULL = 'NUL'
    else
      NULL = '/dev/null'
    end

    FNM_SYSCASE = File::FNM_SYSCASE
  end

  FFI = Rubinius::FFI

  SEPARATOR = FFI::Platform::File::SEPARATOR
  Separator = FFI::Platform::File::SEPARATOR
  ALT_SEPARATOR = FFI::Platform::File::ALT_SEPARATOR
  PATH_SEPARATOR = FFI::Platform::File::PATH_SEPARATOR
  POSIX = Truffle::POSIX

  attr_reader :path

  # The mode_t type is 2 bytes (ushort). Instead of getting whatever
  # value happens to be in the least significant 16 bits, just set
  # the value to 0 if it is greater than 0xffff. Also, negative values
  # don't make any sense here.
  def clamp_short(value)
    mode = Rubinius::Type.coerce_to value, Integer, :to_int
    mode < 0 || mode > 0xffff ? 0 : mode
  end
  module_function :clamp_short

  def self.absolute_path(obj, dir = nil)
    obj = path(obj)
    if obj[0] == "~"
      File.join Dir.getwd, dir.to_s, obj
    else
      expand_path(obj, dir)
    end
  end

  ##
  # Returns the last access time for the named file as a Time object).
  #
  #  File.atime("testfile")   #=> Wed Apr 09 08:51:48 CDT 2003
  def self.atime(path)
    Stat.new(path).atime
  end

  ##
  # Returns the last component of the filename given
  # in file_name, which must be formed using forward
  # slashes (``/’’) regardless of the separator used
  # on the local file system. If suffix is given and
  # present at the end of file_name, it is removed.
  #
  #  File.basename("/home/gumby/work/ruby.rb")          #=> "ruby.rb"
  #  File.basename("/home/gumby/work/ruby.rb", ".rb")   #=> "ruby"
  def self.basename(path, ext=undefined)
    path = Rubinius::Type.coerce_to_path(path)

    slash = "/"

    ext_not_present = undefined.equal?(ext)

    if pos = path.find_string_reverse(slash, path.bytesize)
      # special case. If the string ends with a /, ignore it.
      if pos == path.bytesize - 1

        # Find the first non-/ from the right
        data = path.bytes
        found = false
        pos.downto(0) do |i|
          if data[i] != 47  # ?/
            path = path.byteslice(0, i+1)
            found = true
            break
          end
        end

        # edge case, it's all /'s, return "/"
        return slash unless found

        # Now that we've trimmed the /'s at the end, search again
        pos = path.find_string_reverse(slash, path.bytesize)
        if ext_not_present and !pos
          # No /'s found and ext not present, return path.
          return path
        end
      end

      path = path.byteslice(pos + 1, path.bytesize - pos) if pos
    end

    return path if ext_not_present

    # special case. if ext is ".*", remove any extension

    ext = StringValue(ext)

    if ext == ".*"
      if pos = path.find_string_reverse(".", path.bytesize)
        return path.byteslice(0, pos)
      end
    elsif pos = path.find_string_reverse(ext, path.bytesize)
      # Check that ext is the last thing in the string
      if pos == path.bytesize - ext.size
        return path.byteslice(0, pos)
      end
    end

    return path
  end

  ##
  # Returns true if the named file is a block device.
  def self.blockdev?(path)
    st = Stat.stat path
    st ? st.blockdev? : false
  end

  ##
  # Returns true if the named file is a character device.
  def self.chardev?(path)
    st = Stat.stat path
    st ? st.chardev? : false
  end

  ##
  # Changes permission bits on the named file(s) to
  # the bit pattern represented by mode_int. Actual
  # effects are operating system dependent (see the
  # beginning of this section). On Unix systems, see
  # chmod(2) for details. Returns the number of files processed.
  #
  #  File.chmod(0644, "testfile", "out")   #=> 2
  def self.chmod(mode, *paths)
    mode = clamp_short mode

    paths.each do |path|
      n = POSIX.chmod Rubinius::Type.coerce_to_path(path), mode
      Errno.handle if n == -1
    end
    paths.size
  end

  ##
  # Equivalent to File.chmod, but does not follow symbolic
  # links (so it will change the permissions associated with
  # the link, not the file referenced by the link).
  # Often not available.
  def self.lchmod(mode, *paths)

    mode = Rubinius::Type.coerce_to(mode, Integer, :to_int)

    paths.each do |path|
      n = POSIX.lchmod Rubinius::Type.coerce_to_path(path), mode
      Errno.handle if n == -1
    end

    paths.size
  end

  ##
  # Changes the owner and group of the
  # named file(s) to the given numeric owner
  # and group id‘s. Only a process with superuser
  # privileges may change the owner of a file. The
  # current owner of a file may change the file‘s
  # group to any group to which the owner belongs.
  # A nil or -1 owner or group id is ignored.
  # Returns the number of files processed.
  #
  #  File.chown(nil, 100, "testfile")
  def self.chown(owner, group, *paths)
    if owner
      owner = Rubinius::Type.coerce_to(owner, Integer, :to_int)
    else
      owner = -1
    end

    if group
      group = Rubinius::Type.coerce_to(group, Integer, :to_int)
    else
      group = -1
    end

    paths.each do |path|
      n = POSIX.chown Rubinius::Type.coerce_to_path(path), owner, group
      Errno.handle if n == -1
    end

    paths.size
  end

  def chmod(mode)
    mode = Rubinius::Type.coerce_to(mode, Integer, :to_int)
    n = POSIX.fchmod @descriptor, clamp_short(mode)
    Errno.handle if n == -1
    n
  end

  def chown(owner, group)
    if owner
      owner = Rubinius::Type.coerce_to(owner, Integer, :to_int)
    else
      owner = -1
    end

    if group
      group = Rubinius::Type.coerce_to(group, Integer, :to_int)
    else
      group = -1
    end

    n = POSIX.fchown @descriptor, owner, group
    Errno.handle if n == -1
    n
  end

  ##
  # Equivalent to File.chown, but does not follow
  # symbolic links (so it will change the owner
  # associated with the link, not the file referenced
  # by the link). Often not available. Returns number
  # of files in the argument list.
  def self.lchown(owner, group, *paths)
    raise NotImplementedError, "lchown not implemented on this platform" unless Rubinius::HAVE_LCHOWN

    if owner
      owner = Rubinius::Type.coerce_to(owner, Integer, :to_int)
    else
      owner = -1
    end

    if group
      group = Rubinius::Type.coerce_to(group, Integer, :to_int)
    else
      group = -1
    end

    paths.each do |path|
      n = POSIX.lchown Rubinius::Type.coerce_to_path(path), owner, group
      Errno.handle if n == -1
    end

    paths.size
  end

  def self.mkfifo(path, mode = 0666)
    mode = Rubinius::Type.coerce_to mode, Integer, :to_int
    path = Rubinius::Type.coerce_to_path(path)
    status = Truffle::POSIX.mkfifo(path, mode)
    Errno.handle path if status != 0
    status
  end

  ##
  # Returns the change time for the named file (the
  # time at which directory information about the
  # file was changed, not the file itself).
  #
  #  File.ctime("testfile")   #=> Wed Apr 09 08:53:13 CDT 2003
  def self.ctime(path)
    Stat.new(path).ctime
  end

  ##
  # Returns true if the named file is a directory, false otherwise.
  #
  # File.directory?(".")
  def self.directory?(io_or_path)
    io = Rubinius::Type.try_convert io_or_path, IO, :to_io

    if io.is_a? IO
      Stat.fstat(io.fileno).directory?
    else
      st = Stat.stat io_or_path
      st ? st.directory? : false
    end
  end

  def self.last_nonslash(path, start=nil)
    # Find the first non-/ from the right
    data = path.bytes
    start ||= (path.size - 1)

    start.downto(0) do |i|
      if data[i] != 47  # ?/
        return i
      end
    end

    return nil
  end

  ##
  # Returns all components of the filename given in
  # file_name except the last one. The filename must be
  # formed using forward slashes (``/’’) regardless of
  # the separator used on the local file system.
  #
  #  File.dirname("/home/gumby/work/ruby.rb")   #=> "/home/gumby/work"
  def self.dirname(path)
    path = Rubinius::Type.coerce_to_path(path)

    # edge case
    return "." if path.empty?

    slash = "/"

    # pull off any /'s at the end to ignore
    chunk_size = last_nonslash(path)
    return "/" unless chunk_size

    if pos = path.find_string_reverse(slash, chunk_size)
      return "/" if pos == 0

      path = path.byteslice(0, pos)

      return "/" if path == "/"

      return path unless path.suffix? slash

      # prune any trailing /'s
      idx = last_nonslash(path, pos)

      # edge case, only /'s, return /
      return "/" unless idx

      return path.byteslice(0, idx - 1)
    end

    return "."
  end

  ##
  # Returns true if the named file is executable by the
  # effective user id of this process.
  def self.executable?(path)
    st = Stat.stat path
    st ? st.executable? : false
  end

  ##
  # Returns true if the named file is executable by
  # the real user id of this process.
  def self.executable_real?(path)
    st = Stat.stat path
    st ? st.executable_real? : false
  end

  ##
  # Return true if the named file exists.
  def self.exist?(path)
    st = Stat.stat(path)
    st ? true : false
  end

  # Pull a constant for Dir local to File so that we don't have to depend
  # on the global Dir constant working. This sounds silly, I know, but it's a
  # little bit of defensive coding so Rubinius can run things like fakefs better.
  PrivateDir = ::Dir

  ##
  # Converts a pathname to an absolute pathname. Relative
  # paths are referenced from the current working directory
  # of the process unless dir_string is given, in which case
  # it will be used as the starting point. The given pathname
  # may start with a ``~’’, which expands to the process owner‘s
  # home directory (the environment variable HOME must be set
  # correctly). "~user" expands to the named user‘s home directory.
  #
  #  File.expand_path("~oracle/bin")           #=> "/home/oracle/bin"
  #  File.expand_path("../../bin", "/tmp/x")   #=> "/bin"
  def self.expand_path(path, dir=nil)
    path = Rubinius::Type.coerce_to_path(path)
    str = "".force_encoding path.encoding
    first = path[0]
    if first == ?~
      case path[1]
      when ?/
        unless home = ENV["HOME"]
          raise ArgumentError, "couldn't find HOME environment variable when expanding '~'"
        end

        path = ENV["HOME"] + path.byteslice(1, path.bytesize - 1)
      when nil
        unless home = ENV["HOME"]
          raise ArgumentError, "couldn't find HOME environment variable when expanding '~'"
        end

        if home.empty?
          raise ArgumentError, "HOME environment variable is empty expanding '~'"
        end

        return home.dup
      else
        unless length = path.find_string("/", 1)
          length = path.bytesize
        end

        name = path.byteslice 1, length - 1
        unless dir = Rubinius.get_user_home(name)
          raise ArgumentError, "user #{name} does not exist"
        end

        path = dir + path.byteslice(length, path.bytesize - length)
      end
    elsif first != ?/
      if dir
        dir = expand_path dir
      else
        dir = PrivateDir.pwd
      end

      path = "#{dir}/#{path}"
    end

    items = []
    start = 0
    size = path.bytesize

    while index = path.find_string("/", start) or (start < size and index = size)
      length = index - start

      if length > 0
        item = path.byteslice start, length

        if item == ".."
          items.pop
        elsif item != "."
          items << item
        end
      end

      start = index + 1
    end

    if items.empty?
      str << "/"
    else
      items.each { |x| str.append "/#{x}" }
    end

    str
  end

  ##
  # Returns the extension (the portion of file name in
  # path after the period).
  #
  #  File.extname("test.rb")         #=> ".rb"
  #  File.extname("a/b/d/test.rb")   #=> ".rb"
  #  File.extname("test")            #=> ""
  #  File.extname(".profile")        #=> ""
  def self.extname(path)
    path = Rubinius::Type.coerce_to_path(path)
    path_size = path.bytesize

    dot_idx = path.find_string_reverse(".", path_size)

    # No dots at all
    return "" unless dot_idx

    slash_idx = path.find_string_reverse("/", path_size)

    # pretend there is / just to the left of the start of the string
    slash_idx ||= -1

    # no . in the last component of the path
    return "" if dot_idx < slash_idx

    # last component starts with a .
    return "" if dot_idx == slash_idx + 1

    # last component ends with a .
    return "" if dot_idx == path_size - 1

    return path.byteslice(dot_idx, path_size - dot_idx)
  end

  ##
  # Returns true if the named file exists and is a regular file.
  def self.file?(path)
    st = Stat.stat path
    st ? st.file? : false
  end

  def self.braces(pattern, flags=0, patterns=[])
    escape = (flags & FNM_NOESCAPE) == 0

    rbrace = nil
    lbrace = nil

    # Do a quick search for a { to start the search better
    i = pattern.index("{")

    if i
      nest = 0

      while i < pattern.size
        char = pattern[i]

        if char == "{"
          lbrace = i if nest == 0
          nest += 1
        end

        if char == "}"
          nest -= 1
        end

        if nest == 0
          rbrace = i
          break
        end

        if char == "\\" and escape
          i += 1
        end

        i += 1
      end
    end

    # There was a full {} expression detected, expand each part of it
    # recursively.
    if lbrace and rbrace
      pos = lbrace
      front = pattern[0...lbrace]
      back = pattern[(rbrace + 1)..-1]

      while pos < rbrace
        nest = 0
        pos += 1
        last = pos

        while pos < rbrace and not (pattern[pos] == "," and nest == 0)
          nest += 1 if pattern[pos] == "{"
          nest -= 1 if pattern[pos] == "}"

          if pattern[pos] == "\\" and escape
            pos += 1
            break if pos == rbrace
          end

          pos += 1
        end

        brace_pattern = "#{front}#{pattern[last...pos]}#{back}"
        patterns << brace_pattern

        braces(brace_pattern, flags, patterns)
      end
    end
    patterns
  end

  ##
  # Returns true if path matches against pattern The pattern
  # is not a regular expression; instead it follows rules
  # similar to shell filename globbing. It may contain the
  # following metacharacters:
  #
  # *:  Matches any file. Can be restricted by other values in the glob. * will match all files; c* will match all files beginning with c; *c will match all files ending with c; and c will match all files that have c in them (including at the beginning or end). Equivalent to / .* /x in regexp.
  # **:  Matches directories recursively or files expansively.
  # ?:  Matches any one character. Equivalent to /.{1}/ in regexp.
  # [set]:  Matches any one character in set. Behaves exactly like character sets in Regexp, including set negation ([^a-z]).
  # <code></code>:  Escapes the next metacharacter.
  # flags is a bitwise OR of the FNM_xxx parameters. The same glob pattern and flags are used by Dir.glob.
  #
  #  File.fnmatch('cat',       'cat')        #=> true  : match entire string
  #  File.fnmatch('cat',       'category')   #=> false : only match partial string
  #  File.fnmatch('c{at,ub}s', 'cats')       #=> false : { } isn't supported
  #  File.fnmatch('c{at,ub}s', 'cats', File::FNM_EXTGLOB)       #=> true : { } is supported with FNM_EXTGLOB
  #
  #  File.fnmatch('c?t',     'cat')          #=> true  : '?' match only 1 character
  #  File.fnmatch('c??t',    'cat')          #=> false : ditto
  #  File.fnmatch('c*',      'cats')         #=> true  : '*' match 0 or more characters
  #  File.fnmatch('c*t',     'c/a/b/t')      #=> true  : ditto
  #  File.fnmatch('ca[a-z]', 'cat')          #=> true  : inclusive bracket expression
  #  File.fnmatch('ca[^t]',  'cat')          #=> false : exclusive bracket expression ('^' or '!')
  #
  #  File.fnmatch('cat', 'CAT')                     #=> false : case sensitive
  #  File.fnmatch('cat', 'CAT', File::FNM_CASEFOLD) #=> true  : case insensitive
  #
  #  File.fnmatch('?',   '/', File::FNM_PATHNAME)  #=> false : wildcard doesn't match '/' on FNM_PATHNAME
  #  File.fnmatch('*',   '/', File::FNM_PATHNAME)  #=> false : ditto
  #  File.fnmatch('[/]', '/', File::FNM_PATHNAME)  #=> false : ditto
  #
  #  File.fnmatch('\?',   '?')                       #=> true  : escaped wildcard becomes ordinary
  #  File.fnmatch('\a',   'a')                       #=> true  : escaped ordinary remains ordinary
  #  File.fnmatch('\a',   '\a', File::FNM_NOESCAPE)  #=> true  : FNM_NOESACPE makes '\' ordinary
  #  File.fnmatch('[\?]', '?')                       #=> true  : can escape inside bracket expression
  #
  #  File.fnmatch('*',   '.profile')                      #=> false : wildcard doesn't match leading
  #  File.fnmatch('*',   '.profile', File::FNM_DOTMATCH)  #=> true    period by default.
  #  File.fnmatch('.*',  '.profile')                      #=> true
  #
  #  rbfiles = '**' '/' '*.rb' # you don't have to do like this. just write in single string.
  #  File.fnmatch(rbfiles, 'main.rb')                    #=> false
  #  File.fnmatch(rbfiles, './main.rb')                  #=> false
  #  File.fnmatch(rbfiles, 'lib/song.rb')                #=> true
  #  File.fnmatch('**.rb', 'main.rb')                    #=> true
  #  File.fnmatch('**.rb', './main.rb')                  #=> false
  #  File.fnmatch('**.rb', 'lib/song.rb')                #=> true
  #  File.fnmatch('*',           'dave/.profile')                      #=> true
  #
  #  pattern = '*' '/' '*'
  #  File.fnmatch(pattern, 'dave/.profile', File::FNM_PATHNAME)  #=> false
  #  File.fnmatch(pattern, 'dave/.profile', File::FNM_PATHNAME | File::FNM_DOTMATCH) #=> true
  #
  #  pattern = '**' '/' 'foo'
  #  File.fnmatch(pattern, 'a/b/c/foo', File::FNM_PATHNAME)     #=> true
  #  File.fnmatch(pattern, '/a/b/c/foo', File::FNM_PATHNAME)    #=> true
  #  File.fnmatch(pattern, 'c:/a/b/c/foo', File::FNM_PATHNAME)  #=> true
  #  File.fnmatch(pattern, 'a/.b/c/foo', File::FNM_PATHNAME)    #=> false
  #  File.fnmatch(pattern, 'a/.b/c/foo', File::FNM_PATHNAME | File::FNM_DOTMATCH) #=> true

  def self.fnmatch(pattern, path, flags=0)
    pattern = StringValue(pattern)
    path    = Rubinius::Type.coerce_to_path(path)
    flags   = Rubinius::Type.coerce_to(flags, Fixnum, :to_int)
    brace_match = false

    if (flags & FNM_EXTGLOB) != 0
      brace_match = braces(pattern, flags).any? { |p| super(p, path, flags) }
    end
    brace_match || super(pattern, path, flags)
  end

  ##
  # Identifies the type of the named file; the return string is
  # one of "file", "directory", "characterSpecial",
  # "blockSpecial", "fifo", "link", "socket", or "unknown".
  #
  #  File.ftype("testfile")            #=> "file"
  #  File.ftype("/dev/tty")            #=> "characterSpecial"
  #  File.ftype("/tmp/.X11-unix/X0")   #=> "socket"
  def self.ftype(path)
    lstat(path).ftype
  end

  ##
  # Returns true if the named file exists and the effective
  # group id of the calling process is the owner of the file.
  # Returns false on Windows.
  def self.grpowned?(path)
    begin
      lstat(path).grpowned?
    rescue
      false
    end
  end

  ##
  # Returns true if the named files are identical.
  #
  #   open("a", "w") {}
  #   p File.identical?("a", "a")      #=> true
  #   p File.identical?("a", "./a")    #=> true
  #   File.link("a", "b")
  #   p File.identical?("a", "b")      #=> true
  #   File.symlink("a", "c")
  #   p File.identical?("a", "c")      #=> true
  #   open("d", "w") {}
  #   p File.identical?("a", "d")      #=> false
  def self.identical?(orig, copy)
    orig = Rubinius::Type.coerce_to_path(orig)
    st_o = File::Stat.stat(orig)
    copy = Rubinius::Type.coerce_to_path(copy)
    st_c = File::Stat.stat(copy)

    return false if st_o.nil? || st_c.nil?

    return false unless st_o.dev == st_c.dev
    return false unless st_o.ino == st_c.ino
    return false unless st_o.ftype == st_c.ftype
    return false unless POSIX.access(orig, Constants::R_OK)
    return false unless POSIX.access(copy, Constants::R_OK)

    true
  end

  ##
  # Returns a new string formed by joining the strings using File::SEPARATOR.
  #
  #  File.join("usr", "mail", "gumby")   #=> "usr/mail/gumby"
  def self.join(*args)
    return '' if args.empty?

    sep = SEPARATOR

    # The first one is unrolled out of the loop to remove a condition
    # from the loop. It seems needless, but you'd be surprised how much hinges
    # on the performance of File.join
    #
    first = args.shift
    case first
    when String
      first = first.dup
    when Array
      recursion = Thread.detect_recursion(first) do
        first = join(*first)
      end

      raise ArgumentError, "recursive array" if recursion
    else
      # We need to use dup here, since it's possible that
      # StringValue gives us a direct object we shouldn't mutate
      first = Rubinius::Type.coerce_to_path(first).dup
    end

    ret = first

    args.each do |el|
      value = nil

      case el
      when String
        value = el
      when Array
        recursion = Thread.detect_recursion(el) do
          value = join(*el)
        end

        raise ArgumentError, "recursive array" if recursion
      else
        value = Rubinius::Type.coerce_to_path(el)
      end

      if value.prefix? sep
        ret.gsub!(/#{SEPARATOR}+$/o, '')
      elsif not ret.suffix? sep
        ret << sep
      end

      ret << value
    end
    ret
  end

  ##
  # Creates a new name for an existing file using a hard link.
  # Will not overwrite new_name if it already exists (raising
  # a subclass of SystemCallError). Not available on all platforms.
  #
  #  File.link("testfile", ".testfile")   #=> 0
  #  IO.readlines(".testfile")[0]         #=> "This is line one\n"
  def self.link(from, to)
    n = POSIX.link Rubinius::Type.coerce_to_path(from), Rubinius::Type.coerce_to_path(to)
    Errno.handle if n == -1
    n
  end

  ##
  # Same as File.stat, but does not follow the last symbolic link.
  # Instead, reports on the link itself.
  #
  #  File.symlink("testfile", "link2test")   #=> 0
  #  File.stat("testfile").size              #=> 66
  #  File.lstat("link2test").size            #=> 8
  #  File.stat("link2test").size             #=> 66
  def self.lstat(path)
    Stat.lstat path
  end

  ##
  # Returns the modification time for the named file as a Time object.
  #
  #  File.mtime("testfile")   #=> Tue Apr 08 12:58:04 CDT 2003
  def self.mtime(path)
    Stat.new(path).mtime
  end

  def self.path(obj)
    return obj.to_path if obj.respond_to? :to_path

    StringValue(obj)
  end

  ##
  # Returns true if the named file is a pipe.
  def self.pipe?(path)
    st = Stat.stat path
    st ? st.pipe? : false
  end

  ##
  # Returns true if the named file is readable by the effective
  # user id of this process.
  def self.readable?(path)
    st = Stat.stat path
    st ? st.readable? : false
  end

  ##
  # Returns true if the named file is readable by the real user
  # id of this process.
  def self.readable_real?(path)
    st = Stat.stat path
    st ? st.readable_real? : false
  end

  ##
  # Returns the name of the file referenced by the given link.
  # Not available on all platforms.
  #
  #  File.symlink("testfile", "link2test")   #=> 0
  #  File.readlink("link2test")              #=> "testfile"
  def self.readlink(path)
    FFI::MemoryPointer.new(1024) do |ptr|
      n = POSIX.readlink Rubinius::Type.coerce_to_path(path), ptr, 1024
      Errno.handle if n == -1

      return ptr.read_string(n)
    end
  end

  def self.realpath(path, basedir = nil)
    real = basic_realpath path, basedir

    unless exist? real
      raise Errno::ENOENT, real
    end

    real
  end

  def self.realdirpath(path, basedir = nil)
    real = basic_realpath path, basedir
    dir = dirname real

    unless directory? dir
      raise Errno::ENOENT, real
    end

    real
  end

  def self.basic_realpath(path, basedir = nil)
    path = expand_path(path, basedir || Dir.pwd)
    real = ''
    symlinks = {}

    while !path.empty?
      pos = path.index(SEPARATOR, 1)

      if pos
        name = path[0...pos]
        path = path[pos..-1]
      else
        name = path
        path = ''
      end

      real = join(real, name)
      if symlink?(real)
        raise Errno::ELOOP if symlinks[real]
        symlinks[real] = true
        if path.empty?
          path = expand_path(readlink(real), dirname(real))
        else
          path = expand_path(join(readlink(real), path), dirname(real))
        end
        real = ''
      end
    end

    real
  end
  private_class_method :basic_realpath

  ##
  # Renames the given file to the new name. Raises a SystemCallError
  # if the file cannot be renamed.
  #
  #  File.rename("afile", "afile.bak")   #=> 0
  def self.rename(from, to)
    n = POSIX.rename Rubinius::Type.coerce_to_path(from), Rubinius::Type.coerce_to_path(to)
    Errno.handle if n == -1
    n
  end

  ##
  # Returns the size of file_name.
  def self.size(io_or_path)
    io = Rubinius::Type.try_convert io_or_path, IO, :to_io

    if io.is_a? IO
      Stat.fstat(io.fileno).size
    else
      stat(io_or_path).size
    end
  end

  ##
  # Returns nil if file_name doesn‘t exist or has zero size,
  # the size of the file otherwise.
  def self.size?(io_or_path)
    s = 0

    io = Rubinius::Type.try_convert io_or_path, IO, :to_io

    if io.is_a? IO
      s = Stat.fstat(io.fileno).size
    else
      st = Stat.stat io_or_path
      s = st.size if st
    end

    s > 0 ? s : nil
  end

  ##
  # Returns true if the named file is a socket.
  def self.socket?(path)
    st = Stat.stat path
    st ? st.socket? : false
  end

  ##
  # Splits the given string into a directory and a file component and returns them in a two-element array.
  # See also File.dirname and File.basename.
  #
  #  File.split("/home/gumby/.profile")   #=> ["/home/gumby", ".profile"]
  def self.split(path)
    p = Rubinius::Type.coerce_to_path(path)
    [dirname(p), basename(p)]
  end

  ##
  # Returns a File::Stat object for the named file (see File::Stat).
  #
  #  File.stat("testfile").mtime   #=> Tue Apr 08 12:58:04 CDT 2003
  def self.stat(path)
    Stat.new path
  end

  ##
  # Creates a symbolic link called new_name for the
  # existing file old_name. Raises a NotImplemented
  # exception on platforms that do not support symbolic links.
  #
  #  File.symlink("testfile", "link2test")   #=> 0
  def self.symlink(from, to)
    n = POSIX.symlink Rubinius::Type.coerce_to_path(from), Rubinius::Type.coerce_to_path(to)
    Errno.handle if n == -1
    n
  end

  ##
  # Returns true if the named file is a symbolic link.
  def self.symlink?(path)
    Stat.lstat(path).symlink?
  rescue Errno::ENOENT, Errno::ENODIR
    false
  end

  ##
  # Truncates the file file_name to be at most integer
  # bytes long. Not available on all platforms.
  #
  #  f = File.new("out", "w")
  #  f.write("1234567890")     #=> 10
  #  f.close                   #=> nil
  #  File.truncate("out", 5)   #=> 0
  #  File.size("out")          #=> 5
  def self.truncate(path, length)
    path = Rubinius::Type.coerce_to_path(path)

    unless exist?(path)
      raise Errno::ENOENT, path
    end

    length = Rubinius::Type.coerce_to length, Integer, :to_int

    prim_truncate(path, length)
  end

  ##
  # Returns the current umask value for this process.
  # If the optional argument is given, set the umask
  # to that value and return the previous value. Umask
  # values are subtracted from the default permissions,
  # so a umask of 0222 would make a file read-only for
  # everyone.
  #
  #  File.umask(0006)   #=> 18
  #  File.umask         #=> 6
  def self.umask(mask = nil)
    if mask
      POSIX.umask clamp_short(mask)
    else
      old_mask = POSIX.umask(0)
      POSIX.umask old_mask
      old_mask
    end
  end

  ##
  # Deletes the named files, returning the number of names
  # passed as arguments. Raises an exception on any error.
  #
  # See also Dir.rmdir.
  def self.unlink(*paths)
    paths.each do |path|
      n = POSIX.unlink Rubinius::Type.coerce_to_path(path)
      Errno.handle if n == -1
    end

    paths.size
  end

  ##
  # Sets the access and modification times of each named
  # file to the first two arguments. Returns the number
  # of file names in the argument list.
  #  #=> Integer
  def self.utime(a_in, m_in, *paths)
    a_in ||= Time.now
    m_in ||= Time.now
    FFI::MemoryPointer.new(POSIX::TimeVal, 2) do |ptr|
      atime = POSIX::TimeVal.new ptr
      mtime = POSIX::TimeVal.new ptr[1]
      atime[:tv_sec] = a_in.to_i
      atime[:tv_usec] = 0

      mtime[:tv_sec] = m_in.to_i
      mtime[:tv_usec] = 0

      paths.each do |path|

        n = POSIX.utimes(Rubinius::Type.coerce_to_path(path), ptr)
        Errno.handle unless n == 0
      end
    end
  end

  def self.world_readable?(path)
    path = Rubinius::Type.coerce_to_path path
    return nil unless exist? path
    mode = Stat.new(path).mode
    if (mode & Stat::S_IROTH) == Stat::S_IROTH
      tmp = mode & (Stat::S_IRUGO | Stat::S_IWUGO | Stat::S_IXUGO)
      return Rubinius::Type.coerce_to tmp, Fixnum, :to_int
    end
    nil
  end

  def self.world_writable?(path)
    path = Rubinius::Type.coerce_to_path path
    return nil unless exist? path
    mode = Stat.new(path).mode
    if (mode & Stat::S_IWOTH) == Stat::S_IWOTH
      tmp = mode & (Stat::S_IRUGO | Stat::S_IWUGO | Stat::S_IXUGO)
      return Rubinius::Type.coerce_to tmp, Fixnum, :to_int
    end
  end

  ##
  # Returns true if the named file is writable by the effective
  # user id of this process.
  def self.writable?(path)
    st = Stat.stat path
    st ? st.writable? : false
  end

  ##
  # Returns true if the named file is writable by the real user
  # id of this process.
  def self.writable_real?(path)
    st = Stat.stat path
    st ? st.writable_real? : false
  end

  ##
  # Returns true if the named file exists and has a zero size.
  def self.zero?(path)
    st = Stat.stat path
    st ? st.zero? : false
  end

  ##
  # Returns true if the named file exists and the effective
  # used id of the calling process is the owner of the file.
  #  File.owned?(file_name)   => true or false
  def self.owned?(file_name)
    Stat.new(file_name).owned?
  rescue Errno::ENOENT
    return false
  end

  ##
  # Returns true if the named file has the setgid bit set.
  def self.setgid?(file_name)
    Stat.new(file_name).setgid?
  rescue Errno::ENOENT
    return false
  end

  ##
  # Returns true if the named file has the setuid bit set.
  def self.setuid?(file_name)
    Stat.new(file_name).setuid?
  rescue Errno::ENOENT
    return false
  end

  ##
  # Returns true if the named file has the sticky bit set.
  def self.sticky?(file_name)
    Stat.new(file_name).sticky?
  rescue Errno::ENOENT
    return false
  end

  class << self
    alias_method :delete,   :unlink
    alias_method :exists?,  :exist?
    alias_method :fnmatch?, :fnmatch
  end

  def initialize(path_or_fd, mode=nil, perm=undefined, options=undefined)
    if path_or_fd.kind_of? Integer
      super(path_or_fd, mode, options)
      @path = nil
    else
      path = Rubinius::Type.coerce_to_path path_or_fd

      # TODO: fix normalize_options
      case mode
      when String, Fixnum
        # do nothing
      when nil
        mode = "r"
      when Hash
        options = mode
        mode = nil
      else
        options = Rubinius::Type.coerce_to mode, Hash, :to_hash
        mode = nil
      end

      if undefined.equal?(options) and !undefined.equal?(perm)
        options = Rubinius::Type.try_convert(perm, Hash, :to_hash)
        perm = undefined if options
      end

      nmode, binary, external, internal = IO.normalize_options(mode, options)
      nmode ||= "r"

      perm = 0666 if undefined.equal? perm

      fd = IO.sysopen(path, nmode, perm)
      Errno.handle path if fd < 0

      @path = path
      super(fd, mode, options)
    end
  end

  private :initialize

  def atime
    Stat.new(@path).atime
  end

  def reopen(other, mode = 'r+')
    unless other.kind_of? IO
      other = Rubinius::Type.coerce_to_path(other)
    end
    super(other, mode)
  end

  def ctime
    Stat.new(@path).ctime
  end

  def flock(const)
    const = Rubinius::Type.coerce_to const, Integer, :to_int

    result = POSIX.flock @descriptor, const

    return false if result == -1
    result
  end

  def lstat
    Stat.lstat @path
  end

  def mtime
    Stat.new(@path).mtime
  end

  def stat
    Stat.fstat @descriptor
  end

  alias_method :to_path, :path

  def truncate(length)
    length = Rubinius::Type.coerce_to length, Integer, :to_int

    ensure_open_and_writable
    raise Errno::EINVAL, "Can't truncate a file to a negative length" if length < 0

    flush
    reset_buffering
    prim_ftruncate(length)
  end

  def inspect
    return_string = "#<#{self.class}:0x#{object_id.to_s(16)} path=#{@path}"
    return_string << " (closed)" if closed?
    return_string << ">"
  end

  def size
    raise IOError, "closed stream" if closed?
    stat.size
  end
end     # File

# Inject the constants into IO
class IO
  include File::Constants
end

File::Stat = Rubinius::Stat
class File::Stat
  @module_name = :"File::Stat"

  def world_readable?
    if mode & S_IROTH == S_IROTH
      tmp = mode & (S_IRUGO | S_IWUGO | S_IXUGO)
      return Rubinius::Type.coerce_to tmp, Fixnum, :to_int
    end
  end

  def world_writable?
    if mode & S_IWOTH == S_IWOTH
      tmp = mode & (S_IRUGO | S_IWUGO | S_IXUGO)
      return Rubinius::Type.coerce_to tmp, Fixnum, :to_int
    end
  end
end

if Truffle::Safe.io_safe?
  STDIN = File.new(0)
  STDOUT = File.new(1)
  STDERR = File.new(2)
else
  STDIN = nil
  STDOUT = nil
  STDERR = nil
end

$stdin = STDIN
$stdout = STDOUT
$stderr = STDERR

class << STDIN
  def external_encoding
    super || Encoding.default_external
  end
end

if Truffle::Safe.io_safe?
  if STDOUT.tty?
    STDOUT.sync = true
  else
    Truffle::Kernel.at_exit true do
      STDOUT.flush
    end
  end

  if STDERR.tty?
    STDERR.sync = true
  else
    Truffle::Kernel.at_exit true do
      STDERR.flush
    end
  end
end
