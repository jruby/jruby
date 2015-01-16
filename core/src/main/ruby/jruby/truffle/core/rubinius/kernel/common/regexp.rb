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

# Only part of Rubinius' regexp.rb

class Regexp

  IGNORECASE         = 1
  EXTENDED           = 2
  MULTILINE          = 4
  FIXEDENCODING      = 16
  NOENCODING         = 32

  def self.convert(pattern)
    return pattern if pattern.kind_of? Regexp
    if pattern.kind_of? Array
      return union(*pattern)
    else
      return Regexp.quote(pattern.to_s)
    end
  end

  def self.compatible?(*patterns)
    encodings = patterns.map{ |r| convert(r).encoding }
    last_enc = encodings.pop
    encodings.each do |encoding|
      raise ArgumentError, "incompatible encodings: #{encoding} and #{last_enc}" unless Encoding.compatible?(last_enc, encoding)
      last_enc = encoding
    end
  end

  def self.union(*patterns)
    case patterns.size
      when 0
        return %r/(?!)/
      when 1
        pat = patterns.first
        case pat
          when Array
            return union(*pat)
          when Regexp
            return pat
          else
            return Regexp.new(Regexp.quote(StringValue(pat)))
        end
      else
        compatible?(*patterns)
        enc = convert(patterns.first).encoding
    end

    str = "".encode(enc)
    sep = "|".encode(enc)
    patterns.each_with_index do |pat, idx|
      str << sep if idx != 0
      if pat.kind_of? Regexp
        str << pat.to_s
      else
        str << Regexp.quote(StringValue(pat))
      end
    end

    Regexp.new(str)
  end

end

class MatchData

  def inspect
    matched_area = values_at(0).first # This was added to support Truffle, which doesn't have a matched_area method and we can't readily uses Rubinius's.
    capts = captures
    if capts.empty?
      "#<MatchData \"#{matched_area}\">"
    else
      idx = 0
      capts.map! { |capture| "#{idx += 1}:#{capture.inspect}" }
      "#<MatchData \"#{matched_area}\" #{capts.join(" ")}>"
    end
  end

end