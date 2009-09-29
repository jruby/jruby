require 'rbconfig'
raise  LoadError.new("Win32API only supported on win32") unless Config::CONFIG['host_os'] =~ /mswin/

require 'ffi'

class Win32API
  CONVENTION = FFI::Platform.windows? ? :stdcall : :default
  SUFFIXES = $KCODE == 'UTF8' ? [ '', 'W', 'A' ] : [ '', 'A', 'W' ]
  TypeDefs = {
    'V' => :void,
    'P' => :pointer,
    'I' => :int,
    'N' => :int,
    'L' => :int,
  }

  def self.find_type(name)
    code = TypeDefs[name]
    raise TypeError, "Unable to resolve type '#{name}'" unless code
    return code
  end

  def self.map_types(spec)
    types = []
    if spec.kind_of?(String)
      spec.each_byte { |c|types << self.find_type(c.chr.upcase) }
    elsif spec.kind_of?(Array)
      spec.each { |c|types << self.find_type(c.upcase) }
    end
    types
  end

  def initialize(lib, func, params, ret='L')
    @lib = lib
    @func = func
    @params = params
    @return = ret

    #
    # Attach the method as 'call', so it gets all the froody arity-splitting optimizations
    #
    extend FFI::Library
    ffi_lib lib
    ffi_convention CONVENTION
    attached = false
    SUFFIXES.each { |suffix|
      begin
        attach_function(:call, func.to_s + suffix, Win32API.map_types(params), Win32API.find_type(ret))
        self.instance_eval("alias :Call :call")
        attached = true
        break
      rescue FFI::NotFoundError => ex
      end
    }
    raise FFI::NotFoundError, "Could not locate #{func}" unless attached
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