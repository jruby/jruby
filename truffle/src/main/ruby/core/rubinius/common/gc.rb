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

module GC
  def self.count
    data = stat
    data[:"gc.young.count"] + data[:"gc.immix.count"]
  end

  def self.time
    data = stat
    data[:"gc.young.ms"] +
      data[:"gc.immix.stop.ms"] +
      data[:"gc.large.sweep.us"] * 1_000
  end

  def self.stat
    Rubinius::Metrics.data.to_hash
  end

  module Profiler
    @enabled = true
    @since   = 0

    def self.clear
      @since = GC.time
      nil
    end

    def self.disable
      # Treat this like a request that we don't honor.
      ret = !@enabled
      @enabled = false
      ret
    end

    def self.enable
      # We don't support disable, so sure! enabled!
      ret = !@enabled
      @enabled = true
      ret
    end

    def self.enabled?
      @enabled
    end

    def self.report(out = $stdout)
      out.write result
    end

    def self.result
      stats = GC.stat

      out = <<-OUT
Complete process runtime statistics
===================================

Collections
                         Count       Total time / concurrent (ms)
Young   #{sprintf("% 22d", stats[:'gc.young.count'])} #{sprintf("% 16d             ", stats[:'gc.young.ms'])}
Full    #{sprintf("% 22d", stats[:'gc.immix.count'])} #{sprintf("% 16d / % 10d", stats[:'gc.immix.stop.ms'], stats[:'gc.immix.concurrent.ms'])}

Allocation
             Objects allocated        Bytes allocated
Young   #{sprintf("% 22d", stats[:'memory.young.objects'])} #{sprintf("% 22d", stats[:'memory.young.bytes'])}
Promoted#{sprintf("% 22d", stats[:'memory.promoted.objects'])} #{sprintf("% 22d", stats[:'memory.promoted.bytes'])}
Mature  #{sprintf("% 22d", stats[:'memory.immix.objects'])} #{sprintf("% 22d", stats[:'memory.immix.bytes'])}


Usage
                    Bytes used
Young   #{sprintf("% 22d", stats[:'memory.young.bytes'])}
Mature  #{sprintf("% 22d", stats[:'memory.immix.bytes'])}
Large   #{sprintf("% 22d", stats[:'memory.large.bytes'])}
Code    #{sprintf("% 22d", stats[:'memory.code.bytes'])}
Symbols #{sprintf("% 22d", stats[:'memory.symbols.bytes'])}
      OUT
    end

    def self.total_time
      (GC.time - @since) / 1000.0
    end
  end
end
