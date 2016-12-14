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

module Rubinius
  module ThrownValue
    def self.register(obj)
      cur = (Thread.current[:__catches__] ||= [])
      cur << obj

      begin
        yield
      ensure
        cur.pop
      end
    end

    def self.available?(obj)
      cur = Thread.current[:__catches__]
      return false unless cur
      cur.each do |c|
        return true if Rubinius::Type.object_equal(c, obj)
      end
      false
    end
  end
end

module Kernel
  def catch(obj = Object.new, &block)
    raise LocalJumpError unless block_given?

    Rubinius::ThrownValue.register(obj) do
      return Rubinius.catch(obj, block)
    end
  end
  module_function :catch

  def throw(obj, value=nil)
    unless Rubinius::ThrownValue.available? obj
      raise UncaughtThrowError, "uncaught throw #{obj.inspect}"
    end

    Rubinius.throw obj, value
  end
  module_function :throw
end
