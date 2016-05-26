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

module FileTest
  def blockdev?(path)
    File.blockdev? path
  end

  def chardev?(path)
    File.chardev? path
  end

  def directory?(path)
    File.directory? path
  end

  def executable?(path)
    File.executable? path
  end

  def executable_real?(path)
    File.executable_real? path
  end

  def exist?(path)
    File.exist? path
  end
  alias_method :exists?, :exist?

  def file?(path)
    File.file? path
  end

  def grpowned?(path)
    File.grpowned? path
  end

  def identical?(a, b)
    File.identical? a, b
  end

  def owned?(path)
    File.owned? path
  end

  def pipe?(path)
    File.pipe? path
  end

  def readable?(path)
    File.readable? path
  end

  def readable_real?(path)
    File.readable_real? path
  end

  def setgid?(path)
    File.setgid? path
  end

  def setuid?(path)
    File.setuid? path
  end

  def size(path)
    File.size path
  end

  def size?(path)
    File.size? path
  end

  def socket?(path)
    File.socket? path
  end

  def sticky?(path)
    File.sticky? path
  end

  def symlink?(path)
    File.symlink? path
  end

  def writable?(path)
    File.writable? path
  end

  def writable_real?(path)
    File.writable_real? path
  end

  def zero?(path)
    File.zero? path
  end

  module_function :blockdev?,
                  :chardev?,
                  :directory?,
                  :executable?,
                  :executable_real?,
                  :exist?,
                  :exists?,
                  :file?,
                  :grpowned?,
                  :identical?,
                  :owned?,
                  :pipe?,
                  :readable?,
                  :readable_real?,
                  :setgid?,
                  :setuid?,
                  :size,
                  :size?,
                  :socket?,
                  :sticky?,
                  :symlink?,
                  :writable?,
                  :writable_real?,
                  :zero?
end
