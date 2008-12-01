module FFI::Library
  # TODO: Rubinius does *names here and saves the array. Multiple libs?
  def ffi_lib(*names)
    @ffi_lib = names
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
    libraries = defined?(@ffi_lib) ? @ffi_lib : [ nil ]
    convention = defined?(@ffi_convention) ? @ffi_convention : :default

    # Convert :foo to the native type
    arg_types = arg_types.map { |e|
      begin
        find_type(e)
      rescue FFI::TypeError => ex
        if defined?(@ffi_callbacks) && @ffi_callbacks.has_key?(e)
          @ffi_callbacks[e]
        elsif e.is_a?(Class) && e < FFI::Struct
          FFI::NativeType::POINTER
        else
          raise ex
        end
      end
    }
    options = Hash.new
    options[:convention] = convention
    options[:type_map] = @ffi_typedefs if defined?(@ffi_typedefs)
    # Try to locate the function in any of the libraries
    invoker = libraries.collect do |lib|
      begin
        FFI.create_invoker lib, cname.to_s, arg_types, find_type(ret_type), options
      rescue LoadError => ex
        nil
      end
    end.compact.shift
    raise FFI::NotFoundError.new(cname.to_s, libraries) unless invoker
    invoker.attach(self.class, mname.to_s)
    invoker.attach(self, mname.to_s)
    # Return a callable version of the invoker
    Module.new do
      invoker.attach(self, "call")
      extend self
    end
  end

  def callback(name, args, ret)
    @ffi_callbacks = Hash.new unless defined?(@ffi_callbacks)
    @ffi_callbacks[name] = FFI::CallbackInfo.new(find_type(ret), args.map { |e| find_type(e) })
  end
  def typedef(current, add)
    @ffi_typedefs = Hash.new unless defined?(@ffi_typedefs)
    if current.kind_of? Integer
      code = current
    else
      code = @ffi_typedefs[current] || FFI.find_type(current)
    end

    @ffi_typedefs[add] = code
  end
  def find_type(name)
    code = if defined?(@ffi_typedefs)
      @ffi_typedefs[name]
    end
    code = name if !code && name.kind_of?(FFI::CallbackInfo)
    if code.nil? || code.kind_of?(Symbol)
      FFI.find_type(name)
    else
      code
    end
  end
end
