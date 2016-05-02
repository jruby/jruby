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

class Exception

  # Indicates if the Exception has a backtrace set
  def backtrace?
    backtrace ? true : false # Truffle: simplified
  end

  def set_backtrace(bt)
    if false # bt.kind_of? Rubinius::Backtrace # Truffle: not supported
      @backtrace = bt
    else
      # See if we stashed a Backtrace object away, and use it.
      if false # hidden_bt = Rubinius::Backtrace.detect_backtrace(bt) # Truffle: not yet supported
        @backtrace = hidden_bt
      else
        type_error = TypeError.new "backtrace must be Array of String"
        case bt
          when Array
            if bt.all? { |s| s.kind_of? String }
              @custom_backtrace = bt
            else
              raise type_error
            end
          when String
            @custom_backtrace = [bt]
          when nil
            @custom_backtrace = nil
          else
            raise type_error
        end
      end
    end
  end

end
