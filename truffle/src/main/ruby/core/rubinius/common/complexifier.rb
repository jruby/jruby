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

class String
  class Complexifier
    SPACE = Rationalizer::SPACE
    NUMERATOR = Rationalizer::NUMERATOR
    DENOMINATOR = Rationalizer::DENOMINATOR
    NUMBER = "[-+]?#{NUMERATOR}(?:\\/#{DENOMINATOR})?"
    NUMBERNOS = "#{NUMERATOR}(?:\\/#{DENOMINATOR})?"
    PATTERN0 = Regexp.new "\\A#{SPACE}(#{NUMBER})@(#{NUMBER})#{SPACE}"
    PATTERN1 = Regexp.new "\\A#{SPACE}([-+])?(#{NUMBER})?[iIjJ]#{SPACE}"
    PATTERN2 = Regexp.new "\\A#{SPACE}(#{NUMBER})(([-+])(#{NUMBERNOS})?[iIjJ])?#{SPACE}"

    def initialize(value)
      @value = value
    end

    def convert
      if m = PATTERN0.match(@value)
        sr = m[1]
        si = m[2]
        re = m.post_match
        po = true
      elsif m = PATTERN1.match(@value)
        sr = nil
        si = (m[1] || "") + (m[2] || "1")
        re = m.post_match
        po = false
      elsif m = PATTERN2.match(@value)
        sr = m[1]
        si = m[2] ? m[3] + (m[4] || "1") : nil
        re = m.post_match
        po = false
      else
        return Complex.new(0, 0)
      end

      r = 0
      i = 0

      if sr
        if sr.include?("/")
          r = sr.to_r
        elsif sr.match(/[.eE]/)
          r = sr.to_f
        else
          r = sr.to_i
        end
      end

      if si
        if si.include?("/")
          i = si.to_r
        elsif si.match(/[.eE]/)
          i = si.to_f
        else
          i = si.to_i
        end
      end

      if po
        Complex.polar(r, i)
      else
        Complex.rect(r, i)
      end
    end
  end
end
