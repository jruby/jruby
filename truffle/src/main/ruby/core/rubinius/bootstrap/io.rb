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

class IO

  class InternalBuffer
    def self.allocate
      Rubinius.primitive :iobuffer_allocate
      raise PrimitiveFailure, "IO::Buffer.allocate primitive failed"
    end

    ##
    # Returns the number of bytes that could be written to the buffer.
    # If the number is less then the expected, then we need to +empty_to+
    # the IO, and +unshift+ again beginning at +start_pos+.
    def unshift(str, start_pos)
      Rubinius.primitive :iobuffer_unshift
      raise PrimitiveFailure, "IO::Buffer#unshift primitive failed"
    end

    def fill(io)
      Rubinius.primitive :iobuffer_fill

      unless io.kind_of? IO
        return fill(io.to_io)
      end

      raise PrimitiveFailure, "IOBuffer#fill primitive failed"
    end
  end

  def self.allocate
    Rubinius.primitive :io_allocate
    raise PrimitiveFailure, "IO.allocate primitive failed"
  end

  def self.open_with_mode(path, mode, perm)
    Rubinius.primitive :io_open
    raise PrimitiveFailure, "IO.open_with_mode primitive failed"
  end

  def self.connect_pipe(lhs, rhs)
    Rubinius.primitive :io_connect_pipe
    raise PrimitiveFailure, "IO.connect_pipe primitive failed"
  end

  def self.select_primitive(readables, writables, errorables, timeout)
    Rubinius.primitive :io_select
    raise IOError, "Unable to select on IO set (descriptor too big?)"
  end

  def self.fnmatch(pattern, path, flags)
    Rubinius.primitive :io_fnmatch
    raise PrimitiveFailure, "IO#fnmatch primitive failed"
  end

  # Instance primitive bindings

  def ensure_open
    Rubinius.primitive :io_ensure_open
    raise PrimitiveFailure, "IO#ensure_open primitive failed"
  end

  def read_primitive(number_of_bytes)
    Rubinius.primitive :io_sysread
    raise PrimitiveFailure, "IO::sysread primitive failed"
  end

  def write(str)
    Rubinius.primitive :io_write
    raise PrimitiveFailure, "IO#write primitive failed"
  end

  def read_if_available(size)
    Rubinius.primitive :io_read_if_available
    raise PrimitiveFailure, "IO#read_if_available primitive failed"
  end

  def raw_write(str)
    Rubinius.primitive :io_write_nonblock
    raise PrimitiveFailure, "IO#write_nonblock primitive failed"
  end

  def reopen_io(other)
    Rubinius.primitive :io_reopen
    raise ArgumentError, "IO#prim_reopen only accepts an IO object"
  end

  def reopen_path(string, mode)
    Rubinius.primitive :io_reopen_path

    if mode.kind_of? Bignum
      raise ArgumentError, "Bignum too big for mode"
    end

    reopen_path StringValue(string), Integer(mode)
  end

  def prim_seek(amount, whence)
    Rubinius.primitive :io_seek
    raise RangeError, "#{amount} is too big"
  end

  def self.prim_truncate(name, offset)
    Rubinius.primitive :io_truncate
    raise RangeError, "#{offset} is too big"
  end

  def prim_ftruncate(offset)
    Rubinius.primitive :io_ftruncate
    raise RangeError, "#{amount} is too big"
  end

  def query(which)
    Rubinius.primitive :io_query
    raise PrimitiveFailure, "IO#query primitive failed"
  end

  def reopen(other)
    reopen_io other
  end

  def tty?
    query :tty?
  end

  def ttyname
    query :ttyname
  end

  def close
    Rubinius.primitive :io_close
    raise PrimitiveFailure, "IO#close primitive failed"
  end

  #
  # Close read and/or write stream of a full-duplex descriptor.
  #
  # @todo   More documentation. Much more. --rue
  #
  def shutdown(how)
    Rubinius.primitive :io_shutdown
    raise PrimitiveFailure, "IO#innertShutdown primitive failed"
  end

  def socket_recv(bytes, flags, type)
    Rubinius.primitive :io_socket_read
    raise PrimitiveFailure, "io_socket_read failed"
  end

  module Socketable
    def accept
      Rubinius.primitive :io_accept
      raise PrimitiveFailure, "io_accept failed"
    end
  end

  module TransferIO
    def send_io
      Rubinius.primitive :io_send_io
      raise PrimitiveFailure, "IO#send_io failed"
    end

    def recv_fd
      Rubinius.primitive :io_recv_fd
      raise PrimitiveFailure, "IO#recv_fd failed"
    end
  end

end
