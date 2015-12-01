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

# Only part of Rubinius' alpha.rb

module Process
  # Terminate with given status code.
  #
  def self.exit(code=0)
    case code
    when true
      code = 0
    when false
      code = 1
    else
      code = Rubinius::Type.coerce_to code, Integer, :to_int
    end

    raise SystemExit.new(code)
  end

  def self.exit!(code=1)
    Rubinius.primitive :vm_exit

    case code
    when true
      exit! 0
    when false
      exit! 1
    else
      exit! Rubinius::Type.coerce_to(code, Integer, :to_int)
    end
  end
end

class Module

  # :internal:
  #
  # Basic version of .include used in kernel code.
  #
  # Redefined in kernel/delta/module.rb.
  #
  def include(mod)
    Rubinius.privately do
      mod.append_features self # Truffle: moved the append_features inside the privately
      mod.included self
    end
    self
  end

end

module Kernel

  # Rubinius defines this method differently, using the :object_class primitive.  The two primitives are very similar,
  # so rather than introduce the new one, we'll just delegate to the existing one.
  def __class__
    Rubinius.invoke_primitive :vm_object_class, self
  end

end
