# Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1
#
# Contains code adapted from platform/library.rb in Rubinius
#
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

# This is part of the Rubinius FFI implementation that links to native
# libraries. We simply hook up a method which we already provide, so it only
# works for libraries we are expecting.

module Rubinius::FFI::Library

  def attach_function(name, a2, a3, a4=nil, a5=nil)
    # Argument handling from Rubinius

    if a4 && (a2.kind_of?(String) || a2.kind_of?(Symbol))
      cname = a2.to_s
      args = a3
      ret = a4
    else
      cname = name.to_s
      args = a2
      ret = a3
    end

    mname = name.to_sym

    # The difference is we already have the methods available in our version of
    # the POSIX class

    caller = Truffle::Primitive.source_of_caller

    if caller.end_with? 'ruby/truffle/rubysl/rubysl-socket/lib/rubysl/socket.rb'
      if Rubinius::FFI::Platform::POSIX.respond_to? mname
        define_method mname, Rubinius::FFI::Platform::POSIX.method(mname)
        module_function mname
        return
      end
    end

    # Fallback

    define_method mname do |*|
      raise "FFI::Library method #{name} with caller #{caller} not implemented"
    end
    module_function mname
  end

end
