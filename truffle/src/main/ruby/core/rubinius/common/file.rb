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

# Only part of Rubinius' file.rb

class File < IO
  include Enumerable

  module Constants
    FNM_NOESCAPE = 0x01
    FNM_PATHNAME = 0x02
    FNM_DOTMATCH = 0x04
    FNM_CASEFOLD = 0x08
    FNM_EXTGLOB  = 0x10
  end

  FFI = Rubinius::FFI
  
  POSIX = FFI::Platform::POSIX

  ##
  # Return true if the named file exists.
  def self.exist?(path)
    st = Stat.stat(path)
    st ? true : false
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

  ##
  # Returns true if the named file exists and is a regular file.
  def self.file?(path)
    st = Stat.stat path
    st ? st.file? : false
  end

  ##
  # Returns true if the named file is executable by the
  # effective user id of this process.
  def self.executable?(path)
    st = Stat.stat path
    st ? st.executable? : false
  end

  ##
  # Returns true if the named file is readable by the effective
  # user id of this process.
  def self.readable?(path)
    st = Stat.stat path
    st ? st.readable? : false
  end

  ##
  # Deletes the named files, returning the number of names
  # passed as arguments. Raises an exception on any error.
  #
  # See also Dir::rmdir.
  def self.unlink(*paths)
    paths.each do |path|
      n = POSIX.unlink Rubinius::Type.coerce_to_path(path)
      Errno.handle if n == -1
    end

    paths.size
  end

  class << self
    alias_method :delete,   :unlink
    alias_method :exists?,  :exist?
    alias_method :fnmatch?, :fnmatch
  end

  def self.path(obj)
    return obj.to_path if obj.respond_to? :to_path

    StringValue(obj)
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
        data = path.data
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
  # flags is a bitwise OR of the FNM_xxx parameters. The same glob pattern and flags are used by Dir::glob.
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
  # Returns a File::Stat object for the named file (see File::Stat).
  #
  #  File.stat("testfile").mtime   #=> Tue Apr 08 12:58:04 CDT 2003
  def self.stat(path)
    Stat.new path
  end

  def self.last_nonslash(path, start=nil)
    # Find the first non-/ from the right
    data = path.data
    idx = nil
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

  def self.absolute_path(obj, dir = nil)
    obj = path(obj)
    if obj[0] == "~"
      File.join Dir.getwd, dir.to_s, obj
    else
      expand_path(obj, dir)
    end
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
        ret.gsub!(/#{SEPARATOR}+$/, '')
      elsif not ret.suffix? sep
        ret << sep
      end

      ret << value
    end
    ret
  end

  def self.realpath(path, basedir = nil)
    real = basic_realpath path, basedir

    unless exist? real
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
  # Returns true if the named file is a symbolic link.
  def self.symlink?(path)
    Stat.lstat(path).symlink?
  rescue Errno::ENOENT, Errno::ENODIR
    false
  end

end

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
