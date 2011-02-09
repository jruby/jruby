# Version: CPL 1.0/GPL 2.0/LGPL 2.1
#
# The contents of this file are subject to the Common Public
# License Version 1.0 (the "License"); you may not use this file
# except in compliance with the License. You may obtain a copy of
# the License at http://www.eclipse.org/legal/cpl-v10.html
#
# Software distributed under the License is distributed on an "AS
# IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
# implied. See the License for the specific language governing
# rights and limitations under the License.
#
# Copyright (C) 2011 Charles Oliver Nutter <headius@headius.com>
#
# Alternatively, the contents of this file may be used under the terms of
# either of the GNU General Public License Version 2 or later (the "GPL"),
# or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
# in which case the provisions of the GPL or the LGPL are applicable instead
# of those above. If you wish to allow use of your version of this file only
# under the terms of either the GPL or the LGPL, and not to allow others to
# use your version of this file under the terms of the CPL, indicate your
# decision by deleting the provisions above and replace them with the notice
# and other provisions required by the GPL or the LGPL. If you do not delete
# the provisions above, a recipient may use your version of this file under
# the terms of any one of the CPL, the GPL or the LGPL.

require 'ffi'

module Fiddle
  TYPE_VOID         = FFI::Type::Builtin::VOID
  TYPE_VOIDP        = FFI::Type::Builtin::POINTER
  TYPE_CHAR         = FFI::Type::Builtin::CHAR
  TYPE_SHORT        = FFI::Type::Builtin::SHORT
  TYPE_INT          = FFI::Type::Builtin::INT
  TYPE_LONG         = FFI::Type::Builtin::LONG
  TYPE_LONG_LONG    = FFI::Type::Builtin::LONG_LONG
  TYPE_FLOAT        = FFI::Type::Builtin::FLOAT
  TYPE_DOUBLE       = FFI::Type::Builtin::DOUBLE

  WINDOWS = FFI::Platform.windows?

  class Function
    DEFAULT = "default"
    STDCALL = "stdcall"

    def initialize(ptr, args, return_type, abi = DEFAULT)
      @ptr, @args, @return_type, @abi = ptr, args, return_type, abi

      @function = FFI::Function.new(
        @return_type,
        @args,
        FFI::Pointer.new(@ptr.to_i),
        :convention => @abi
      )
    end

    def call(*args)
      result = @function.call(*args)

      result
    end
  end

  class Closure
    def initialize(ret, args, abi = Function::DEFAULT)
      @ctype, @args = ret, args

      @function = FFI::Function.new(
        @ctype,
        @args,
        self,
        :convention => abi
      )
    end

    def to_i
      @function.to_i
    end
  end
end