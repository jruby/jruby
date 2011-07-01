require 'win32/registry'

class WIN32OLE_TYPELIB
  java_import org.racob.com.TypeLib

  attr_reader :typelib
  attr_reader :name
  alias :to_s :name

  def initialize(*args)
    # TODO: Make this work internally and externally API w/ regards to inargs
    if args.length == 2
      @typelib, @name = *args
      puts "NO TYPELIB! for #{@name} #{@version}" unless @typelib
    elsif args.length == 1
      @name = args[0]
      @typelib = search_registry(@name) # TODO: Missing search_registry2
#      puts "NAME IS #{@name}///#{@typelib}"
    end
  end

  def guid
    @typelib.guid
  end

  def minor_version
    @typelib.minor_version
  end

  def major_version
    @typelib.major_version
  end

  def ole_classes # MRI: ole_types_from_typelib
    ole_classes = []
    find_all_typeinfo(@typelib) do |info, docs|
      ole_classes << WIN32OLE_TYPE.new(self, info, docs)
    end
    ole_classes
  end

  def version
    [minor_version, major_version].join('.')
  end

  def visible?
    flags = @typelib.flags
    flags != 0 && (flags & TypeLib::LIBFLAG_FRESTRICTED) == 0 &&
      (flags & TypeLib::LIBFLAG_FHIDDEN) == 0
  end

  def inspect
    name
  end

  class << self
    def ole_classes(typelib)
      new(typelib).ole_classes
    end

    def typelibs
      typelibs = []
      typelib_registry_each_guid_version do |guid, version, reg|
        name = reg.read(nil)[1] || ''
        registry_subkey(reg, 'win32', 'win64') do |arch_reg, arch|
          type_lib = load_typelib(arch_reg, arch)
          # TODO: I think MRI figures out a few more typelibs than we do
          typelibs << WIN32OLE_TYPELIB.new(type_lib, name) if type_lib
        end
      end
      typelibs
    end

    include WIN32OLE::Utils
  end

  include WIN32OLE::Utils
end
