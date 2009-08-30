module FFI
  TypeDefs = Hash.new
  def self.add_typedef(current, add)
    if current.kind_of?(Type)
      code = current
    else
      code = TypeDefs[current]
      raise TypeError, "Unable to resolve type '#{current}'" unless code
    end

    TypeDefs[add] = code
  end
  def self.find_type(name, type_map = nil)
    type_map = TypeDefs if type_map.nil?
    code = type_map[name]
    code = name if !code && name.kind_of?(FFI::Type)
    raise TypeError, "Unable to resolve type '#{name}'" unless code
    return code
  end

  # Converts a char
  add_typedef(Type::CHAR, :char)

  # Converts an unsigned char
  add_typedef(Type::UCHAR, :uchar)

  # Converts an 8 bit int
  add_typedef(Type::INT8, :int8)

  # Converts an unsigned char
  add_typedef(Type::UINT8, :uint8)

  # Converts a short
  add_typedef(Type::SHORT, :short)

  # Converts an unsigned short
  add_typedef(Type::USHORT, :ushort)

  # Converts a 16bit int
  add_typedef(Type::INT16, :int16)

  # Converts an unsigned 16 bit int
  add_typedef(Type::UINT16, :uint16)

  # Converts an int
  add_typedef(Type::INT, :int)

  # Converts an unsigned int
  add_typedef(Type::UINT, :uint)

  # Converts a 32 bit int
  add_typedef(Type::INT32, :int32)

  # Converts an unsigned 16 bit int
  add_typedef(Type::UINT32, :uint32)

  # Converts a long
  add_typedef(Type::LONG, :long)

  # Converts an unsigned long
  add_typedef(Type::ULONG, :ulong)

  # Converts a 64 bit int
  add_typedef(Type::INT64, :int64)

  # Converts an unsigned 64 bit int
  add_typedef(Type::UINT64, :uint64)

  # Converts a long long
  add_typedef(Type::LONG_LONG, :long_long)

  # Converts an unsigned long long
  add_typedef(Type::ULONG_LONG, :ulong_long)

  # Converts a float
  add_typedef(Type::FLOAT, :float)

  # Converts a double
  add_typedef(Type::DOUBLE, :double)

  # Converts a pointer to opaque data
  add_typedef(Type::POINTER, :pointer)

  # For when a function has no return value
  add_typedef(Type::VOID, :void)

  # Native boolean type
  add_typedef(Type::BOOL, :bool)

  # Converts NUL-terminated C strings
  add_typedef(Type::STRING, :string)

  # Converts FFI::Buffer objects
  add_typedef(Type::BUFFER_IN, :buffer_in)
  add_typedef(Type::BUFFER_OUT, :buffer_out)
  add_typedef(Type::BUFFER_INOUT, :buffer_inout)
  add_typedef(Type::VARARGS, :varargs)

  # Use for a C struct with a char [] embedded inside.
  add_typedef(NativeType::CHAR_ARRAY, :char_array)

  TypeSizes = {
    1 => :char,
    2 => :short,
    4 => :int,
    8 => :long_long,
  }

  def self.size_to_type(size)
    if sz = TypeSizes[size]
      return sz
    end

    # Be like C, use int as the default type size.
    return :int
  end
  def self.type_size(type)
    if type.kind_of?(Type) || (type = find_type(type))
      return type.size
    end
    raise ArgumentError, "Unknown native type"
  end

  # Load all the platform dependent types
  begin
    File.open(File.join(FFI::Platform::CONF_DIR, 'types.conf'), "r") do |f|
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
end