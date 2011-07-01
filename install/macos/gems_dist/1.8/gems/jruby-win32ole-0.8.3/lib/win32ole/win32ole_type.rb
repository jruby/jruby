class WIN32OLE_TYPE
  java_import org.racob.com.TypeInfo
  
  attr_reader :typeinfo

  def initialize(*args)
    case args.length
    when 2 then 
      typelib_name, olename = SafeStringValue(args[0]), SafeStringValue(args[1])
      @typelib = WIN32OLE_TYPELIB.new(typelib_name) # Internal call
      find_all_typeinfo(@typelib.typelib) do |info, docs|
        if (docs.name == olename)
          @typeinfo, @docs = info, docs
          break
        end
      end
      raise WIN32OLERuntimeError.new("not found `#{olename}` in `#{typelib_name}") unless @typeinfo
    when 3 then
      @typelib, @typeinfo, @docs = *args
    else
      raise ArgumentError.new("wrong number of arguments (#{args.length} for 2)")
    end
  end

  def guid
    @typeinfo.guid
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

  def name
    @docs.name
  end

  def major_version
    @typeinfo.major_version
  end

  def minor_version
    @typeinfo.minor_version
  end

  def ole_methods
    members = []
    all_methods(@typeinfo) do |ti, oti, desc, docs, index|
      members << WIN32OLE_METHOD.new(self, ti, oti, desc, docs, index)
      nil
    end
    members
  end

  def ole_type
    case typekind
    when TypeInfo::TYPEKIND_ENUM then
      "Enum"
    when TypeInfo::TYPEKIND_RECORD then
      "Record"
    when TypeInfo::TYPEKIND_MODULE then
      "Module"
    when TypeInfo::TYPEKIND_INTERFACE then
      "Interface"
    when TypeInfo::TYPEKIND_DISPATCH then
      "Dispatch"
    when TypeInfo::TYPEKIND_COCLASS then
      "Class"
    when TypeInfo::TYPEKIND_ALIAS then
      "Alias"
    when TypeInfo::TYPEKIND_UNION then
      "Union"
    when TypeInfo::TYPEKIND_MAX then
      "Max"
    else
      nil
    end
  end

  def progid
    @typeinfo.progid
  end

  def src_type
    return nil if @typeinfo.typekind != TypeInfo::TYPEKIND_ALIAS

    typedesc_value @typeinfo.alias_vt
  end

  def to_s
    name
  end
  
  def variables
    variables = []
    all_vars(@typeinfo) do |desc, name|
      variables << WIN32OLE_VARIABLE.new(self, desc, name)
    end
    variables
  end

  def visible?
    @typeinfo.flags & (TypeInfo::TYPEFLAG_FHIDDEN | TypeInfo::TYPEFLAG_FRESTRICTED) == 0
  end

  def typekind
    @typeinfo.typekind
  end

  class << self
    # This is obsolete, but easy to emulate
    def typelibs
      WIN32OLE_TYPELIB.typelibs.collect {|t| t.name }
    end

    def progids
      array = []
      Win32::Registry::HKEY_CLASSES_ROOT.open('CLSID') do |reg|
        reg.each_key do |clsid, wtime|
          reg.open(clsid) do |clsid_reg|
            clsid_reg.each_key do |key, wtime|
              name = nil
              if key == "ProgID"
                clsid_reg.open(key) {|key_reg| name = key_reg.read(nil)[1] }
              end
              if !name && key == "VersionIndependentProgID"
                clsid_reg.open(key) {|key_reg| name = key_reg.read(nil)[1] }
              end
              array << name if name
            end
          end
        end
      end
      array
    end

    def ole_classes(tlib)
      WIN32OLE_TYPELIB.ole_classes(tlib)
    end
  end

  include WIN32OLE::Utils
end
