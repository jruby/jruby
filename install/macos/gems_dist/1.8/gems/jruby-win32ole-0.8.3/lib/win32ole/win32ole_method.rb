class WIN32OLE_METHOD
  java_import org.racob.com.FuncDesc
  java_import org.racob.com.TypeInfo
  
  attr_accessor :oletype, :typeinfo

  def initialize(*args)
    # TODO: 2-arg missing currently unuised oletype ivar
    if args.length == 6 # Internal initializer
      @oletype,  @typeinfo, @owner_typeinfo, @desc, @docs, @index = *args
    elsif args.length == 2 # Normal constructor
      @oletype, name = WIN32OLE_TYPEValue(args[0]), SafeStringValue(args[1])
      all_methods(@oletype.typeinfo) do |ti, oti, desc, docs, index|
        if docs.name.downcase == name.downcase
          @typeinfo, @owner_typeinfo, @desc, @docs, @index = ti, oti, desc, docs, index
          break;
        end
      end
      raise WIN32OLERuntimeError.new "not found: #{name}" if !@typeinfo
    else # Error
      raise ArgumentError.new("2 for #{args.length}")
    end
  end

  def dispid
    @desc.memid
  end

  def event?
    return false if @owner_typeinfo.typekind != TypeInfo::TYPEKIND_COCLASS

    @owner_typeinfo.impl_types_count.times do |i|
      begin
        flags = @owner_typeinfo.get_impl_type_flags(i)

        if flags & TypeInfo::IMPLTYPEFLAG_FSOURCE
          href = @owner_typeinfo.get_ref_type_of_impl_type(i)
          ref_typeinfo = @owner_typeinfo.get_ref_type_info(href)
          func_desc = ref_typeinfo.func_desc(@index)
          documentation = ref_typeinfo.documentation(func_desc.memid)

          return true if documentation.name == name
        end
      rescue ComFailException => e
      end
    end
    false
  end

  def event_interface
    return nil unless event?

    typelib = @typeinfo.containing_type_lib
    documentation = typelib.documentation(typelib.index)
    documentation.name
  end

  def helpcontext
    @docs.help_context
  end

  def helpstring
    @docs.doc_string
  end

  def helpfile
    @docs.help_file
  end

  def invkind
    @desc.invkind
  end

  def invoke_kind
    invkind = @desc.invkind
    if invkind & Dispatch::Get && invkind & Dispatch::Put
      return "PROPERTY"
    elsif invkind & Dispatch::Get
      return "PROPERTYGET"
    elsif invkind & Dispatch::Put
      return "PROPERTYPUT"
    elsif invkind & Dispatch::PutRef
      return "PROPERTYPUTREF"
    elsif invkind & Dispatch::Method
      return "FUNC"
    else
      return "UNKOWN"
    end
  end

  def name
    @docs.name
  end
  alias :to_s :name

  def offset_vtbl
    @desc.vtable_offset
  end

  def params
    arr = []
    @desc.parameters.to_a.each_with_index do |param, i|
      arr << WIN32OLE_PARAM.new(self, i, param)
    end
    arr
  end

  def return_type
    typedesc_value(@desc.return_type.vt)
  end

  def return_type_detail
    typedesc_value(@desc.return_type.vt, [])
  end

  def return_vtype
    @desc.return_type.vt
  end

  def size_opt_params
    @desc.opt_params_count
  end
  
  def size_params
    @desc.params_count
  end

  def visible?
    (@desc.flags & (FuncDesc::FUNCFLAG_FRESTRICTED | FuncDesc::FUNCFLAG_FHIDDEN |
      FuncDesc::FUNCFLAG_FNONBROWSABLE)) == 0
  end

  def inspect
    name
  end

  include WIN32OLE::Utils
end
