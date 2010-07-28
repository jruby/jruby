module FFI
  CURRENT_PROCESS = USE_THIS_PROCESS_AS_LIBRARY = Object.new

  module Library
    CURRENT_PROCESS = FFI::CURRENT_PROCESS
    LIBC = FFI::Platform::LIBC

    def ffi_lib(*names)
      lib_flags = defined?(@ffi_lib_flags) ? @ffi_lib_flags : FFI::DynamicLibrary::RTLD_LAZY | FFI::DynamicLibrary::RTLD_LOCAL
      ffi_libs = names.map do |name|
        if name == FFI::CURRENT_PROCESS
          FFI::DynamicLibrary.open(nil, FFI::DynamicLibrary::RTLD_LAZY | FFI::DynamicLibrary::RTLD_LOCAL)
        else
          libnames = (name.is_a?(::Array) ? name : [ name ]).map { |n| [ n, FFI.map_library_name(n) ].uniq }.flatten.compact
          lib = nil
          errors = {}

          libnames.each do |libname|
            begin
              lib = FFI::DynamicLibrary.open(libname, lib_flags)
              break if lib
            rescue Exception => ex
              errors[libname] = ex
            end
          end

          if lib.nil?
            raise LoadError.new(errors.values.join('. '))
          end

          # return the found lib
          lib
        end
      end

      @ffi_libs = ffi_libs
    end


    def ffi_convention(convention)
      @ffi_convention = convention
    end


    def ffi_libraries
      raise LoadError.new("no library specified") if !defined?(@ffi_libs) || @ffi_libs.empty?
      @ffi_libs
          end

    FlagsMap = {
      :global => DynamicLibrary::RTLD_GLOBAL,
      :local => DynamicLibrary::RTLD_LOCAL,
      :lazy => DynamicLibrary::RTLD_LAZY,
      :now => DynamicLibrary::RTLD_NOW
    }

    def ffi_lib_flags(*flags)
      lib_flags = flags.inject(0) { |result, f| result | FlagsMap[f] }
      if (lib_flags & (DynamicLibrary::RTLD_LAZY | DynamicLibrary::RTLD_NOW)) == 0
        lib_flags |= DynamicLibrary::RTLD_LAZY
      end

      if (lib_flags & (DynamicLibrary::RTLD_GLOBAL | DynamicLibrary::RTLD_LOCAL) == 0)
        lib_flags |= DynamicLibrary::RTLD_LOCAL
      end

      @ffi_lib_flags = lib_flags
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

    def attach_function(mname, a2, a3, a4=nil, a5 = nil)
      cname, arg_types, ret_type, opts = (a4 && (a2.is_a?(String) || a2.is_a?(Symbol))) ? [ a2, a3, a4, a5 ] : [ mname.to_s, a2, a3, a4 ]


      # Convert :foo to the native type
      arg_types.map! { |e| find_type(e) }
      options = Hash.new
      options[:convention] = defined?(@ffi_convention) ? @ffi_convention : :default
      options[:type_map] = defined?(@ffi_typedefs) ? @ffi_typedefs : nil
      options[:enums] = defined?(@ffi_enum_map) ? @ffi_enum_map : nil
      options.merge!(opts) if opts.is_a?(Hash)

      # Try to locate the function in any of the libraries
      invokers = []
      load_error = nil
      ffi_libraries.each do |lib|
        if invokers.empty?
          begin
            function = lib.find_function(cname.to_s)
            raise NotFoundError.new(name, cname.to_s) unless function
            invokers << if arg_types.length > 0 && arg_types[arg_types.length - 1] == FFI::NativeType::VARARGS
              FFI::VariadicInvoker.new(arg_types, find_type(ret_type), function, options)
            else
              FFI::Function.new(find_type(ret_type), arg_types, function, options)
            end
   
          rescue LoadError => ex
            load_error = ex

          end
        end
      end
      invoker = invokers.compact.shift
      raise load_error if load_error && invoker.nil?
      #raise FFI::NotFoundError.new(cname.to_s, *libraries) unless invoker
      invoker.attach(self, mname.to_s)
      invoker # Return a version that can be called via #call
    end

    def attach_variable(mname, a1, a2 = nil)
      cname, type = a2 ? [ a1, a2 ] : [ mname.to_s, a1 ]
      address = nil
      ffi_libraries.each do |lib|
        begin
          address = lib.find_variable(cname.to_s)
          break unless address.nil?
        rescue LoadError
        end
      end

      raise FFI::NotFoundError.new(cname, ffi_libraries) if address.nil? || address.null?
      
      if type.is_a?(Class) && type < FFI::Struct
        # If it is a global struct, just attach directly to the pointer
        s = type.new(address)
        self.module_eval <<-code, __FILE__, __LINE__
          @@ffi_gvar_#{mname} = s
          def self.#{mname}
            @@ffi_gvar_#{mname}
          end
        code

      else
        sc = Class.new(FFI::Struct)
        sc.layout :gvar, find_type(type)
        s = sc.new(address)
        #
        # Attach to this module as mname/mname=
        #
        self.module_eval <<-code, __FILE__, __LINE__
          @@ffi_gvar_#{mname} = s
          def self.#{mname}
            @@ffi_gvar_#{mname}[:gvar]
          end
          def self.#{mname}=(value)
            @@ffi_gvar_#{mname}[:gvar] = value
          end
        code

      end
      
      address
    end

    def callback(*args)
      raise ArgumentError, "wrong number of arguments" if args.length < 2 || args.length > 3
      name, params, ret = if args.length == 3
        args
      else
        [ nil, args[0], args[1] ]
      end

      options = Hash.new
      options[:convention] = defined?(@ffi_convention) ? @ffi_convention : :default
      options[:enums] = @ffi_enums if defined?(@ffi_enums)

      cb = FFI::CallbackInfo.new(find_type(ret), params.map { |e| find_type(e) }, options)

      # Add to the symbol -> type map (unless there was no name)
      unless name.nil?
        __cb_map[name] = cb

        # Also put in the type map, so it can be used for typedefs
        __type_map[name] = cb
      end

      cb
    end

    def __type_map
      defined?(@ffi_typedefs) ? @ffi_typedefs : (@ffi_typedefs = Hash.new)
    end

    def __cb_map
      defined?(@ffi_callbacks) ? @ffi_callbacks: (@ffi_callbacks = Hash.new)
    end

    def typedef(old, add, info=nil)
      @ffi_typedefs = Hash.new unless defined?(@ffi_typedefs)

      @ffi_typedefs[add] = if old.kind_of?(FFI::Type)
        old

      elsif @ffi_typedefs.has_key?(old)
        @ffi_typedefs[old]

      elsif old.is_a?(DataConverter)
        FFI::Type::Mapped.new(old)

      elsif old == :enum
        if add.kind_of?(Array)
          self.enum(add)
        else
          self.enum(info, add)
        end

      else
        FFI.find_type(old)
      end
    end

    def enum(*args)
      #
      # enum can be called as:
      # enum :zero, :one, :two  # unnamed enum
      # enum [ :zero, :one, :two ] # equivalent to above
      # enum :foo, [ :zero, :one, :two ] create an enum named :foo
      #
      name, values = if args[0].kind_of?(Symbol) && args[1].kind_of?(Array)
        [ args[0], args[1] ]
      elsif args[0].kind_of?(Array)
        [ nil, args[0] ]
      else
        [ nil, args ]
      end
      @ffi_enums = FFI::Enums.new unless defined?(@ffi_enums)
      @ffi_enums << (e = FFI::Enum.new(values, name))
      @ffi_enum_map = Hash.new unless defined?(@ffi_enum_map)
      # append all the enum values to a global :name => value map
      @ffi_enum_map.merge!(e.symbol_map)

      # If called as enum :foo, [ :zero, :one, :two ], add a typedef alias
      typedef(e, name) if name
      e
    end

    def enum_type(name)
      @ffi_enums.find(name) if defined?(@ffi_enums)
    end

    def enum_value(symbol)
      @ffi_enums.__map_symbol(symbol)
    end

    def find_type(t)
      if t.kind_of?(FFI::Type)
        t
      
      elsif defined?(@ffi_typedefs) && @ffi_typedefs.has_key?(t)
        @ffi_typedefs[t]

      elsif defined?(@ffi_callbacks) && @ffi_callbacks.has_key?(t)
        @ffi_callbacks[t]

      elsif t.is_a?(DataConverter)
        # Add a typedef so next time the converter is used, it hits the cache
        typedef Type::Mapped.new(t), t

      elsif t.is_a?(Class) && t < FFI::Struct
        FFI::NativeType::POINTER

      end || FFI.find_type(t)
    end
  end
end
