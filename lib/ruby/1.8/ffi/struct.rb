require 'ffi'

module FFI
  class BaseStruct
    Buffer = FFI::Buffer
    attr_reader :pointer

    def initialize(pointer = nil, *spec)
      @cspec = self.class.layout(*spec)

      if pointer then
        @pointer = pointer
      else
        @pointer = MemoryPointer.new size
      end
    end
    def self.alloc_inout(clear = true)
      self.new(Buffer.__alloc_inout(@size, clear))
    end
    def self.alloc_in(clear = true)
      self.new(Buffer.__alloc_in(@size, clear))
    end
    def self.alloc_out(clear = true)
      self.new(Buffer.__alloc_out(@size, clear))
    end
    def self.size
      @size
    end
    def self.members
      @layout.members
    end
    def self.in
      :buffer_in
    end
    def self.out
      :buffer_out
    end
    def size
      self.class.size
    end
    def [](field)
      @cspec.get(@pointer, field)
    end
    def []=(field, val)
      @cspec.put(@pointer, field, val)
    end
    def members
      @cspec.members
    end
    def values
      @cspec.members.map { |m| self[m] }
    end
    def clear
      @pointer.clear
      self
    end
    def to_ptr
      @pointer
    end
  end
end


module FFI
  class Struct < FFI::BaseStruct
    def self.hash_layout(spec)
        builder = FFI::StructLayoutBuilder.new
        mod = enclosing_module
        spec[0].each do |name,type|
          builder.add_field(name, find_type(type, mod))
        end
        builder.build
    end
    def self.array_layout(spec)
      builder = FFI::StructLayoutBuilder.new
      mod = enclosing_module
      i = 0
      while i < spec.size
        name, type = spec[i, 2]
        i += 2
        code = find_type(type, mod)
        # If the next param is a Integer, it specifies the offset
        if spec[i].kind_of?(Integer)
          offset = spec[i]
          i += 1
          builder.add_field(name, code, offset)
        else
          builder.add_field(name, code)
        end
      end
      builder.build
    end
    def self.layout(*spec)
      return @layout if spec.size == 0

      cspec = spec[0].kind_of?(Hash) ? hash_layout(spec) : array_layout(spec)
      @layout = cspec unless self == FFI::Struct
      @size = cspec.size
      return cspec
    end

    def self.config(base, *fields)
      config = FFI::Config::CONFIG
      @size = config["#{base}.sizeof"]
    
      builder = FFI::StructLayoutBuilder.new
    
      fields.each do |field|
        offset = config["#{base}.#{field}.offset"]
        size   = config["#{base}.#{field}.size"]
        type   = config["#{base}.#{field}.type"]
        type   = type ? type.to_sym : FFI.size_to_type(size)

        code = FFI.find_type type
        if (code == NativeType::CHAR_ARRAY)
          builder.add_char_array(field.to_s, size, offset)
        else
          builder.add_field(field.to_s, code, offset)
        end
      end
      cspec = builder.build
    
      @layout = cspec
      @size = cspec.size if @size < cspec.size
    
      return cspec
    end
    private
    def self.enclosing_module
      begin
        Object.const_get(self.name.split("::")[0..-2].join("::"))
      rescue Exception
        nil
      end
    end
    def self.find_type(type, mod = nil)
      return mod ? mod.find_type(type) : FFI.find_type(type)
    end
  end
end
