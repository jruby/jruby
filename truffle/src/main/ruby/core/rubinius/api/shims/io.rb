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

class IO

  class InternalBuffer
    def initialize
      @start = 0
    end
  end

  # Truffle: redefine setter to lower
  def mode=(value)
    @mode = Truffle::Fixnum.lower(value)
  end

  ##
  # Writes the given objects to ios as with IO#print.
  # Writes a record separator (typically a newline)
  # after any that do not already end with a newline
  # sequence. If called with an array argument, writes
  # each element on a new line. If called without arguments,
  # outputs a single record separator.
  #
  #  $stdout.puts("this", "is", "a", "test")
  # produces:
  #
  #  this
  #  is
  #  a
  #  test
  def puts(*args)
    if args.empty?
      write DEFAULT_RECORD_SEPARATOR
    else
      args.each do |arg|
        if arg.equal? nil
          str = ""
        elsif Thread.guarding? arg
          str = "[...]"
        elsif arg.kind_of?(Array)
          Thread.recursion_guard arg do
            arg.each do |a|
              puts a
            end
          end
        else
          str = arg.to_s
        end

        if str
          # Truffle: write the string + record separator (\n) atomically so multithreaded #puts is bearable
          unless str.suffix?(DEFAULT_RECORD_SEPARATOR)
            str += DEFAULT_RECORD_SEPARATOR
          end
          write str
        end
      end
    end

    nil
  end

end
