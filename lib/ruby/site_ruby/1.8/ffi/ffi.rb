#
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
# Copyright (C) 2008 JRuby project
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
#
# Copyright (c) 2007, 2008 Evan Phoenix
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
# * Neither the name of the Evan Phoenix nor the names of its contributors 
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

require 'rbconfig'
require 'ffi.so' # Load the JRuby implementation class
require 'ffi/platform'
require 'ffi/struct'
require 'ffi/memorypointer'
require 'ffi/buffer'

module FFI
  NativeType = JRuby::FFI::NativeType

  #  Specialised error classes
  class TypeError < RuntimeError; end
  
  class SignatureError < RuntimeError; end
  
  class NotFoundError < RuntimeError
    def initialize(function, library)
      super("Function '#{function}' not found! (Looking in '#{library}' or this process)")
    end
  end
  
  TypeDefs = Hash.new
  def self.add_typedef(current, add)
    if current.kind_of? Integer
      code = current
    else
      code = TypeDefs[current]
      raise TypeError, "Unable to resolve type '#{current}'" unless code
    end

    TypeDefs[add] = code
  end
  def self.find_type(name)
    code = TypeDefs[name]
    raise TypeError, "Unable to resolve type '#{name}'" unless code
    return code
  end
  
  # Converts a char
  add_typedef(NativeType::INT8, :char)

  # Converts an unsigned char
  add_typedef(NativeType::UINT8, :uchar)
  
  # Converts an 8 bit int
  add_typedef(NativeType::INT8, :int8)

  # Converts an unsigned char
  add_typedef(NativeType::UINT8, :uint8)

  # Converts a short
  add_typedef(NativeType::INT16, :short)

  # Converts an unsigned short
  add_typedef(NativeType::UINT16, :ushort)
  
  # Converts a 16bit int
  add_typedef(NativeType::INT16, :int16)

  # Converts an unsigned 16 bit int
  add_typedef(NativeType::UINT16, :uint16)

  # Converts an int
  add_typedef(NativeType::INT32, :int)

  # Converts an unsigned int
  add_typedef(NativeType::UINT32, :uint)
  
  # Converts a 32 bit int
  add_typedef(NativeType::INT32, :int32)

  # Converts an unsigned 16 bit int
  add_typedef(NativeType::UINT32, :uint32)

  # Converts a long
  add_typedef(NativeType::LONG, :long)

  # Converts an unsigned long
  add_typedef(NativeType::ULONG, :ulong)
  
  # Converts a 64 bit int
  add_typedef(NativeType::INT64, :int64)

  # Converts an unsigned 64 bit int
  add_typedef(NativeType::UINT64, :uint64)

  # Converts a long long
  add_typedef(NativeType::INT64, :long_long)

  # Converts an unsigned long long
  add_typedef(NativeType::UINT64, :ulong_long)

  # Converts a float
  add_typedef(NativeType::FLOAT32, :float)

  # Converts a double
  add_typedef(NativeType::FLOAT64, :double)

  # Converts a pointer to opaque data
  add_typedef(NativeType::POINTER, :pointer)

  # For when a function has no return value
  add_typedef(NativeType::VOID, :void)

  # Converts NUL-terminated C strings
  add_typedef(NativeType::STRING, :string)
  
  # Use for a C struct with a char [] embedded inside.
  add_typedef(NativeType::CHAR_ARRAY, :char_array)
  
  # Load all the platform dependent types
  begin
    File.open(File.join(JRuby::FFI::Platform::CONF_DIR, 'types.conf'), "r") do |f|
      prefix = "rbx.platform.typedef."
      f.each_line { |line|
        if line.index(prefix) == 0
          new_type, orig_type = line.chomp.slice(prefix.length..-1).split(/\s*=\s*/)
          add_typedef(orig_type.to_sym, new_type.to_sym)
        end
      }
    end
  rescue Errno::ENOENT
  end
  TypeSizes = {
    1 => :char,
    2 => :short,
    4 => :int,
    8 => :long_long,
  }
end

module JRuby::FFI
  TypeDefs = Hash.new
  
  def self.add_typedef(current, add)
    if current.kind_of? Integer
      code = current
    else
      code = TypeDefs[current] || FFI::TypeDefs[current]
      raise FFI::TypeError, "Unable to resolve type '#{current}'" unless code
    end

    TypeDefs[add] = code
  end

  def self.find_type(name)
    code = TypeDefs[name] || FFI::TypeDefs[name]
    raise FFI::TypeError, "Unable to resolve type '#{name}'" unless code
    return code
  end

  def self.create_invoker(lib, name, args, ret, convention = :default)
    # Ugly hack to simulate the effect of dlopen(NULL, x) - not quite correct
    lib = JRuby::FFI::Platform::LIBC unless lib
    # jna & jffi need just the last part of the library name
    lib = lib[3..-1] if lib =~ /^lib/
    # Current artificial limitation based on JFFI limit
    raise FFI::SignatureError, 'FFI functions may take max 32 arguments!' if args.size > 32

    invoker = InvokerFactory.createInvoker(lib, name, find_type(ret),
      args.map { |e| find_type(e) }, convention.to_s)
    raise FFI::NotFoundError.new(name, lib) unless invoker
    return invoker
  end

  def self.create_function(lib, name, args, ret, convention = :default)
    invoker = create_invoker(lib, name, args, ret, convention)
    Module.new do
      invoker.attach(self, "call")
      extend self
    end
  end
  
  def self.load_library(libname, convention = :default, &block)
    library = NativeLibrary.new libname, convention
    library.instance_eval(&block)
    return library
  end
  class NativeLibrary < Module
    def initialize(libname, convention)
      @ffi_lib = libname
      @ffi_convention = convention
    end
    def method_missing(meth, *args)
      a0, a1, a2 = args
      fname, arg_types, ret_type = args.length > 2 ? [ a0, a1, a2 ] : [ meth.to_sym, a0, a1 ]
      invoker = FFI.create_invoker @ffi_lib, fname.to_s, arg_types, ret_type, @ffi_convention
      raise ArgumentError, "Unable to find function '#{fname}' to bind to #{self.name}.#{meth}" unless invoker
      invoker.attach(self, meth.to_s)
    end
  end
  
  # NUL terminated immutable string
  add_typedef(NativeType::STRING, :string)
  add_typedef(NativeType::STRING, :cstring)
  
  # Converts FFI::Buffer objects
  add_typedef(NativeType::BUFFER_IN, :buffer_in)
  add_typedef(NativeType::BUFFER_OUT, :buffer_out)
  add_typedef(NativeType::BUFFER_INOUT, :buffer_inout)
  
  
  # Load all the platform dependent types/consts/struct members
  class Config
    CONFIG = Hash.new
    begin
      File.open(File.join(Platform::CONF_DIR, 'platform.conf'), "r") do |f|
        typedef = "rbx.platform.typedef."
        f.each_line { |line|
          if line.index(typedef) == 0
            new_type, orig_type = line.chomp.slice(typedef.length..-1).split(/\s*=\s*/)
            FFI.add_typedef(orig_type.to_sym, new_type.to_sym)
          else
            key, value = line.chomp.split(/\s*=\s*/)
            CONFIG[String.new << key] = String.new << value unless key.nil? or value.nil?
          end
        }
      end
    rescue Errno::ENOENT
    end
  end
  
  
  SizeTypes = {
    NativeType::INT8 => 1,
    NativeType::UINT8 => 1,
    NativeType::INT16 => 2,
    NativeType::UINT16 => 2,
    NativeType::INT32 => 4,
    NativeType::UINT32 => 4,
    NativeType::INT64 => 8,
    NativeType::UINT64 => 8,
    NativeType::FLOAT32 => 4,
    NativeType::FLOAT64 => 8,
    NativeType::LONG => JRuby::FFI::Platform::LONG_SIZE / 8,
    NativeType::ULONG => JRuby::FFI::Platform::LONG_SIZE / 8,
  }
  
  def self.size_to_type(size)
    if sz = TypeSizes[size]
      return sz
    end

    # Be like C, use int as the default type size.
    return :int
  end
  def self.type_size(type)
    if sz = SizeTypes[find_type(type)]
      return sz
    end
    raise ArgumentError, "Unknown native type"
  end
  def self.errno
    LastError.error
  end
  def self.set_errno(error)
    LastError.error = error
  end
end
# Define MemoryPointer globally for rubinius FFI backward compatibility
MemoryPointer = JRuby::FFI::MemoryPointer

module FFI
  def self.create_invoker(lib, name, args, ret, convention = :default)
    # Ugly hack to simulate the effect of dlopen(NULL, x) - not quite correct
    lib = JRuby::FFI::Platform::LIBC unless lib
    # jffi needs just the last part of the library name
    lib = lib[3..-1] if lib =~ /^lib/
    
    # Current artificial limitation based on JRuby::FFI limit
    raise SignatureError, 'FFI functions may take max 32 arguments!' if args.size > 32

    invoker = JRuby::FFI::InvokerFactory.createInvoker(lib, name, find_type(ret),
      args.map { |e| find_type(e) }, convention.to_s)
    raise NotFoundError.new(name, lib) unless invoker
    return invoker
  end
  
end
module JRuby::FFI::Library
  # TODO: Rubinius does *names here and saves the array. Multiple libs?
  def ffi_lib(name)
    @ffi_lib = name
  end
  def ffi_convention(convention)
    @ffi_convention = convention
  end
  def jffi_attach(ret_type, name, arg_types, opts = {})
    lib = opts[:from]
    convention = opts[:convention] ? opts[:convention] : :default
    invoker = JRuby::FFI.create_invoker(lib, name.to_s, arg_types, ret_type, convention)
    raise ArgumentError, "Unable to find function '#{name}' to bind to #{self.name}.#{(opts[:as] || name)}" unless invoker
    invoker.attach(self.class, (opts[:as] || name).to_s)
    # Return a callable version of the invoker
    Module.new do
      invoker.attach(self, "call")
      extend self
    end
  end
  def attach_function(mname, a3, a4, a5=nil)
    cname, args, ret = a5 ? [ a3, a4, a5 ] : [ mname.to_sym, a3, a4 ]
    jffi_attach(ret, cname, args, { :as => mname, :from => @ffi_lib, :convention => @ffi_convention })
  end
end
module FFI::Library
  # TODO: Rubinius does *names here and saves the array. Multiple libs?
  def ffi_lib(name)
    @ffi_lib = name
  end
  def ffi_convention(convention)
    @ffi_convention = convention
  end
  ##
  # Attach C function +name+ to this module.
  #
  # If you want to provide an alternate name for the module function, supply
  # it after the +name+, otherwise the C function name will be used.#
  #
  # After the +name+, the C function argument types are provided as an Array.
  #
  # The C function return type is provided last.

  def attach_function(mname, a3, a4, a5=nil)
    cname, arg_types, ret_type = a5 ? [ a3, a4, a5 ] : [ mname.to_s, a3, a4 ]
    lib = @ffi_lib
    convention = @ffi_convention ? @ffi_convention : :default
    invoker = FFI.create_invoker lib, cname.to_s, arg_types, ret_type, convention
    raise ArgumentError, "Unable to find function '#{cname}' to bind to #{self.name}.#{mname}" unless invoker
    invoker.attach(self.class, mname.to_s)
    # Return a callable version of the invoker
    Module.new do
      invoker.attach(self, "call")
      extend self
    end
  end
end

#
# Added for backwards compat until all rubinius code is converted to use 'extend FFI::Library'
#
class Module
  include FFI::Library
  def set_ffi_lib(lib)
    ffi_lib lib
  end
end
