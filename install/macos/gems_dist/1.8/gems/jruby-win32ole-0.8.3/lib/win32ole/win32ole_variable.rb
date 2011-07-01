class WIN32OLE_VARIABLE
  java_import org.racob.com.VarDesc
  
  attr_reader :name

  def initialize(type, var_desc, name)
    @type, @var_desc, @name = type, var_desc, name
  end

  def ole_type
    @type.ole_type # MRI gets from vardesc, but why shouldn't this work?
  end

  def ole_type_detail
    # TODO: Fill in other details when they actually exist
    [@type.ole_type]
  end

  def value
    RubyWIN32OLE.from_variant(JRuby.runtime, @var_desc.constant)
  end

  def variable_kind
    case varkind
    when VarDesc::VAR_PERINSTANCE then
      "PERINSTANCE"
    when VarDesc::VAR_STATIC then
      "STATIC"
    when VarDesc::VAR_CONST then
      "CONSTANT"
    when VarDesc::VAR_DISPATCH then
      "DISPATCH"
    else
      "UNKNOWN"
    end
  end

  def varkind
    @var_desc.varkind
  end

  def inspect
    "#<WIN32OLE_VARIABLE:#{to_s}=#{value.inspect}>"
  end

  alias :to_s :name

  def visible?
    flags = @var_desc.flags
    flags & (VarDesc::VARFLAG_FHIDDEN | VarDesc::VARFLAG_FRESTRICTED | VarDesc::VARFLAG_FNONBROWSABLE) == 0
  end

  include WIN32OLE::Utils
end
