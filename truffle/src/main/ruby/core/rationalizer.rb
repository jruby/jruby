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

# Float#rationalize and String#to_r require complex algorithms. The Regexp
# required for String#to_r cannot be created at class scope when loading the
# kernel because Regexp needs to load after String and making a simpler
# Regexp#initialize for bootstrapping isn't really feasible. So these
# algorithms are located here.
#
class String
  class Rationalizer
    SPACE = "\\s*"
    DIGITS = "(?:[0-9](?:_[0-9]|[0-9])*)"
    NUMERATOR = "(?:#{DIGITS}?\\.)?#{DIGITS}(?:[eE][-+]?#{DIGITS})?"
    DENOMINATOR = DIGITS
    RATIONAL = "\\A#{SPACE}([-+])?(#{NUMERATOR})(?:\\/(#{DENOMINATOR}))?#{SPACE}"
    PATTERN = Regexp.new RATIONAL

    def initialize(value)
      @value = value
    end

    def convert
      if m = PATTERN.match(@value)
        si = m[1]
        nu = m[2]
        de = m[3]
        re = m.post_match

        ifp, exp = nu.split /[eE]/
        ip, fp = ifp.split /\./

        value = Rational.new(ip.to_i, 1)

        if fp
          ctype = Truffle::CType
          i = count = 0
          size = fp.size
          while i < size
            count += 1 if ctype.isdigit fp.getbyte(i)
            i += 1
          end

          l = 10 ** count
          value *= l
          value += fp.to_i
          value = value.quo(l)
        end

        value = -value if si == "-"
        value *= 10 ** exp.to_i if exp
        value = value.quo(de.to_i) if de

        value
      else
        Rational(0, 1)
      end
    end
  end
end
