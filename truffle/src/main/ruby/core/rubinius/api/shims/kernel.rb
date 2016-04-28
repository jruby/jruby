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

module Kernel

  def extend(*modules)
    raise ArgumentError, "wrong number of arguments (0 for 1+)" if modules.empty?

    # Disabled for JRuby+Truffle. The frozen check should be done in `object_extend`.
    # Having the check here breaks nil.extend().
    #Rubinius.check_frozen

    modules.reverse_each do |mod|
      # Truffle: added check
      if !mod.kind_of?(Module) or mod.kind_of?(Class)
        raise TypeError, "wrong argument type #{mod.class} (expected Module)"
      end

      Rubinius.privately do
        mod.extend_object self
      end

      Rubinius.privately do
        mod.extended self
      end
    end
    self
  end

  def load(filename, wrap = false)
    filename = Rubinius::Type.coerce_to_path filename

    # load absolute path
    return Truffle::Kernel.load File.expand_path(filename), wrap if filename.start_with? File::SEPARATOR

    # try relative
    if filename.start_with? '.'
      return Truffle::Kernel.load File.expand_path(File.join(Dir.pwd, filename)), wrap
    end

    # try to find relative path in $LOAD_PATH
    [Dir.pwd, *$LOAD_PATH].each do |dir|
      path = File.expand_path(File.join(dir, filename))
      if File.exist? path
        return Truffle::Kernel.load path, wrap
      end
    end

    # file not found trigger an error
    Truffle::Kernel.load filename, wrap
  end
  module_function :load

end
