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

end

File::Stat = Rubinius::Stat

# Inject the constants into IO
class IO
  include File::Constants
end
