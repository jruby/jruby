class WIN32OLE_PARAM
  attr_accessor :name
  
  def initialize(method, index, param_info=nil)
    raise TypeError.new("1st parameter must be WIN32OLE object") if !method.kind_of? WIN32OLE_METHOD

    @method, @index, @param = method, index, param_info
  end

  def default
    @param.is_default_value ? RubyWIN32OLE.from_variant(JRuby.runtime, @param.get_default_value) : nil
  end

  def input?
    @param.is_in
  end

  def name
    @param.name
  end
  alias :to_s :name
  alias :inspect :name

  def ole_type
    typedesc_value(@param.vt)
  end

  def ole_type_detail
    typedesc_value(@param.vt, [])
  end

  def output?
    @param.is_out
  end

  def optional?
    @param.is_optional
  end

  def retval?
    @param.is_return_value
  end

  include WIN32OLE::Utils
end
