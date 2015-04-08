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
  end

  def self.path(obj)
    return obj.to_path if obj.respond_to? :to_path

    StringValue(obj)
  end

end

File::Stat = Rubinius::Stat
