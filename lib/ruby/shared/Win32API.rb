require 'rbconfig'
raise  LoadError.new("Win32API only supported on win32") unless RbConfig::CONFIG['host_os'] =~ /mswin/

require 'jruby/cffi.jar'

class Win32API
  SUFFIXES = Encoding.default_internal == Encoding::UTF_8 ? [ '', 'W', 'A' ] : [ '', 'A', 'W' ]

  class Pointer
    extend JRuby::CFFI::DataConverter
    native_type JRuby::CFFI::Type::POINTER
    
    def self.to_native(value, ctx = nil)
      if value.kind_of?(Integer)
        JRuby::CFFI::Pointer.new(value)
      elsif value.kind_of?(String)
        JRuby::CFFI::Pointer.from_string(value)
      else
        value
      end
    end

    def self.from_native(value, ctx = nil)
      if !value.null?
        value.read_string
      else
        nil
      end
    end
  end

  TypeDefs = {
    '0' => JRuby::CFFI::Type::VOID,
    'V' => JRuby::CFFI::Type::VOID,
    'P' => JRuby::CFFI::Type::Mapped.new(Pointer),
    'I' => JRuby::CFFI::Type::SINT,
    'N' => JRuby::CFFI::Type::SINT,
    'L' => JRuby::CFFI::Type::SINT,
  }

  def self.find_type(name)
    code = TypeDefs[name] || TypeDefs[name.upcase]
    raise TypeError, "Unable to resolve type '#{name}'" unless code
    return code
  end

  def self.map_types(spec)
    if spec.kind_of?(String)
      spec.split(//)
    elsif spec.kind_of?(Array)
      spec
    else
      raise ArgumentError.new("invalid parameter types specification")
    end.map { |c| self.find_type(c) }
  end

  def self.map_library_name(lib)
    # Mangle the library name to reflect the native library naming conventions
    if lib && File.basename(lib) == lib
      ext = ".#{JRuby::CFFI::Platform::LIBSUFFIX}"
      lib = JRuby::CFFI::Platform::LIBPREFIX + lib unless lib =~ /^#{JRuby::CFFI::Platform::LIBPREFIX}/
      lib += ext unless lib =~ /#{ext}/
    end
    lib
  end
  
  def initialize(lib, func, params, ret='L', calltype = :stdcall)
    @lib = lib
    @func = func
    @params = params
    @return = ret

    #
    # Attach the method as 'call', so it gets all the froody arity-splitting optimizations
    #
    @lib = JRuby::CFFI::DynamicLibrary.open(Win32API.map_library_name(lib), JRuby::CFFI::DynamicLibrary::LAZY | JRuby::CFFI::DynamicLibrary::GLOBAL)
    SUFFIXES.each do |suffix|
      sym = @lib.find_function(func.to_s + suffix)
      if sym
        call_context = JRuby::CFFI::CallContext.new(Win32API.find_type(ret), Win32API.map_types(params), :convention => calltype)
        @ffi_func = JRuby::CFFI::Function.new(call_context, sym)
        @ffi_func.attach(self, :call)
        self.instance_eval("alias :Call :call")
        break
      end
    end
    
    raise LoadError.new("Could not locate #{func}") unless @ffi_func
  end

  def inspect
    params = []
    if @params.kind_of?(String)
      @params.each_byte { |c| params << TypeDefs[c.chr] }
    else
      params = @params.map { |p| TypeDefs[p]}
    end
    "#<Win32API::#{@func} library=#{@lib} function=#{@func} parameters=[ #{params.join(',')} ], return=#{Win32API.find_type(@return)}>"
  end
end
