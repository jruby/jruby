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

module Signal
  Names = {
    "EXIT" => 0
  }

  Numbers = {
    0 => "EXIT"
  }

  # Fill the Names and Numbers Hash.
  SIGNAL_LIST.each do |name, number|
    Names[name] = number
    Numbers[number] = name
  end
  remove_const :SIGNAL_LIST

  # replace CLD with CHLD since CLD is not recognized by in `new sun.misc.Signal("CLD")`
  Numbers[Names['CHLD']] = 'CHLD'

  @threads = {}
  @handlers = {}

  def self.trap(sig, prc=nil, &block)
    sig = sig.to_s if sig.kind_of?(Symbol)

    if sig.kind_of?(String)
      osig = sig

      if sig.prefix? "SIG"
        sig = sig[3..-1]
      end

      unless number = Names[sig]
        raise ArgumentError, "Unknown signal '#{osig}'"
      end
    else
      number = sig.to_i
    end

    # If no command, use the block.
    prc ||= block
    prc = prc.to_s if prc.kind_of?(Symbol)

    case prc
    when "DEFAULT", "SIG_DFL"
      had_old = @handlers.key?(number)
      old = @handlers.delete(number)

      if number != Names["EXIT"]
        Rubinius.watch_signal(Numbers[number], 'DEFAULT') # Truffle: adapted to pass the signal name and simpler arguments
      end

      return "DEFAULT" unless had_old
      return old ? old : nil
    when "IGNORE", "SIG_IGN"
      prc = "IGNORE"
    when nil
      prc = nil
    when "EXIT"
      prc = proc { exit }
    when String
      raise ArgumentError, "Unsupported command '#{prc}'"
    else
      unless prc.respond_to? :call
        raise ArgumentError, "Handler must respond to #call (was #{prc.class})"
      end
    end

    had_old = @handlers.key?(number)

    old = @handlers[number]
    @handlers[number] = prc

    if number != Names["EXIT"]
      Rubinius.watch_signal(Numbers[number], (prc.nil? || prc == 'IGNORE') ? nil : prc) # Truffle: adapted to pass the signal name and simpler arguments
    end

    return "DEFAULT" unless had_old
    return old ? old : nil
  end

  def self.list
    Names.dup
  end

  def self.signame(signo)
    index = Rubinius::Type.coerce_to signo, Fixnum, :to_int

    Numbers[index]
  end
end
