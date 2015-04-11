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

class Rubinius::Stat

  include Comparable

  S_IRUSR  = Rubinius::Config['rbx.platform.file.S_IRUSR']
  S_IWUSR  = Rubinius::Config['rbx.platform.file.S_IWUSR']
  S_IXUSR  = Rubinius::Config['rbx.platform.file.S_IXUSR']
  S_IRGRP  = Rubinius::Config['rbx.platform.file.S_IRGRP']
  S_IWGRP  = Rubinius::Config['rbx.platform.file.S_IWGRP']
  S_IXGRP  = Rubinius::Config['rbx.platform.file.S_IXGRP']
  S_IROTH  = Rubinius::Config['rbx.platform.file.S_IROTH']
  S_IWOTH  = Rubinius::Config['rbx.platform.file.S_IWOTH']
  S_IXOTH  = Rubinius::Config['rbx.platform.file.S_IXOTH']

  S_IRUGO  = S_IRUSR | S_IRGRP | S_IROTH
  S_IWUGO  = S_IWUSR | S_IWGRP | S_IWOTH
  S_IXUGO  = S_IXUSR | S_IXGRP | S_IXOTH

  S_IFMT   = Rubinius::Config['rbx.platform.file.S_IFMT']
  S_IFIFO  = Rubinius::Config['rbx.platform.file.S_IFIFO']
  S_IFCHR  = Rubinius::Config['rbx.platform.file.S_IFCHR']
  S_IFDIR  = Rubinius::Config['rbx.platform.file.S_IFDIR']
  S_IFBLK  = Rubinius::Config['rbx.platform.file.S_IFBLK']
  S_IFREG  = Rubinius::Config['rbx.platform.file.S_IFREG']
  S_IFLNK  = Rubinius::Config['rbx.platform.file.S_IFLNK']
  S_IFSOCK = Rubinius::Config['rbx.platform.file.S_IFSOCK']
  S_IFWHT  = Rubinius::Config['rbx.platform.file.S_IFWHT']
  S_ISUID  = Rubinius::Config['rbx.platform.file.S_ISUID']
  S_ISGID  = Rubinius::Config['rbx.platform.file.S_ISGID']
  S_ISVTX  = Rubinius::Config['rbx.platform.file.S_ISVTX']

  attr_reader :path

  def initialize(path)
    Errno.handle path unless setup(path) == 0
  end

  def self.stat(path)
    stat = allocate
    if Rubinius.privately { stat.setup path } == 0
      stat
    else
      nil
    end
  end

  def self.fstat(fd)
    stat = allocate
    result = Rubinius.privately { stat.fsetup fd }
    Errno.handle "file descriptor #{descriptor}" unless result == 0
    stat
  end

  def self.lstat(path)
    stat = allocate
    result = Rubinius.privately { stat.lsetup path }
    Errno.handle path unless result == 0
    stat
  end

  def blockdev?
    mode & S_IFMT == S_IFBLK
  end

  def chardev?
    mode & S_IFMT == S_IFCHR
  end

  def dev_major
    major = Rubinius::FFI::Platform::POSIX.major dev
    major < 0 ? nil : major
  end

  def dev_minor
    minor = Rubinius::FFI::Platform::POSIX.major dev
    minor < 0 ? nil : minor
  end

  def directory?
    mode & S_IFMT == S_IFDIR
  end

  def executable?
    return true if superuser?
    return mode & S_IXUSR != 0 if owned?
    return mode & S_IXGRP != 0 if grpowned?
    return mode & S_IXOTH != 0
  end

  def executable_real?
    return true if rsuperuser?
    return mode & S_IXUSR != 0 if rowned?
    return mode & S_IXGRP != 0 if rgrpowned?
    return mode & S_IXOTH != 0
  end

  def file?
    mode & S_IFMT == S_IFREG
  end

  def ftype
    if file?
      "file"
    elsif directory?
      "directory"
    elsif chardev?
      "characterSpecial"
    elsif blockdev?
      "blockSpecial"
    elsif pipe?
      "fifo"
    elsif socket?
      "socket"
    elsif symlink?
      "link"
    else
      "unknown"
    end
  end

  def grpowned?
    Process.groups.include?(gid)
  end

  def owned?
    uid == Rubinius::FFI::Platform::POSIX.geteuid
  end

  def pipe?
    mode & S_IFMT == S_IFIFO
  end

  def rdev_major
    major = Rubinius::FFI::Platform::POSIX.major rdev
    major < 0 ? nil : major
  end

  def rdev_minor
    minor = Rubinius::FFI::Platform::POSIX.minor rdev
    minor < 0 ? nil : minor
  end

  def readable?
    return true if superuser?
    return mode & S_IRUSR != 0 if owned?
    return mode & S_IRGRP != 0 if grpowned?
    return mode & S_IROTH != 0
  end

  def readable_real?
    return true if rsuperuser?
    return mode & S_IRUSR != 0 if rowned?
    return mode & S_IRGRP != 0 if rgrpowned?
    return mode & S_IROTH != 0
  end

  def setgid?
    mode & S_ISGID != 0
  end

  def setuid?
    mode & S_ISUID != 0
  end

  def sticky?
    mode & S_ISVTX != 0
  end

  def size?
    size == 0 ? nil : size
  end

  def socket?
    mode & S_IFMT == S_IFSOCK
  end

  def symlink?
    mode & S_IFMT == S_IFLNK
  end

  def writable?
    return true if superuser?
    return mode & S_IWUSR != 0 if owned?
    return mode & S_IWGRP != 0 if grpowned?
    return mode & S_IWOTH != 0
  end

  def writable_real?
    return true if rsuperuser?
    return mode & S_IWUSR != 0 if rowned?
    return mode & S_IWGRP != 0 if rgrpowned?
    return mode & S_IWOTH != 0
  end

  def zero?
    size == 0
  end

  def <=>(other)
    return nil unless other.is_a?(File::Stat)
    self.mtime <=> other.mtime
  end

  def rgrpowned?
    gid == Rubinius::FFI::Platform::POSIX.getgid
  end
  private :rgrpowned?

  def rowned?
    uid == Rubinius::FFI::Platform::POSIX.getuid
  end
  private :rowned?

  def rsuperuser?
    Rubinius::FFI::Platform::POSIX.getuid == 0
  end
  private :rsuperuser?

  def superuser?
    Rubinius::FFI::Platform::POSIX.geteuid == 0
  end
  private :superuser?
end
