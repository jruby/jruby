# Copyright (c) 2007-2015 Evan Phoenix and contributors
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

  ##
  # See Regexp.new. This may be overridden by subclasses.

  def compile(pattern, opts)
    Rubinius.primitive :regexp_initialize
    raise PrimitiveFailure, "Regexp.compile(#{pattern.inspect}, #{opts}) primitive failed"
  end

  private :compile

  def search_region(str, start, finish, forward) # equiv to MRI's re_search
    Rubinius.primitive :regexp_search_region
    raise PrimitiveFailure, "Regexp#search_region primitive failed"
  end

  def match_start(str, offset) # equiv to MRI's re_match
    Rubinius.primitive :regexp_match_start
    raise PrimitiveFailure, "Regexp#match_start primitive failed"
  end

  def search_from(str, offset) # equiv to MRI's rb_reg_search
    Rubinius.primitive :regexp_search_from
    raise PrimitiveFailure, "Regexp#search_from primitive failed"
  end

  def options
    Rubinius.primitive :regexp_options
    raise PrimitiveFailure, "Regexp#options primitive failed"
  end

  def self.last_match(field=nil)
    Rubinius.primitive :regexp_last_match

    return last_match(Integer(field)) if field
    raise PrimitiveFailure, "Regexp#last_match primitive failed"
  end

  def self.last_match=(match)
    Rubinius.primitive :regexp_set_last_match

    unless match.kind_of? MatchData
      raise TypeError, "Expected MatchData, got #{match.inspect}"
    end

    raise PrimitiveFailure, "Regexp#set_last_match primitive failed"
  end

  def self.propagate_last_match
    Rubinius.primitive :regexp_propagate_last_match
    raise PrimitiveFailure, "Regexp#propagate_last_match primitive failed"
  end

  def self.set_block_last_match
    Rubinius.primitive :regexp_set_block_last_match
    raise PrimitiveFailure, "Regexp#set_block_last_match primitive failed"
  end

  def fixed_encoding?
    Rubinius.primitive :regexp_fixed_encoding_p
    raise PrimitiveFailure, "Regexp.fixed_encoding? primitive failed"
  end
end
