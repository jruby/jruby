require 'win32ole/win32ole_variant'

class WIN32OLE
  module Utils
    java_import org.racob.com.ComFailException
    java_import org.racob.com.Dispatch

    def SafeStringValue(str)
      return str if str.kind_of?(::String)
      if str.respond_to?(:to_str)
        str = str.to_str
        return str if str.kind_of?(::String)
      end
      raise TypeError
    end

    def WIN32OLE_TYPEValue(value)
      raise TypeError.new("1st argument should be WIN32OLE_TYPE object") unless value.kind_of? WIN32OLE_TYPE
      value
    end

    def methods_with_flag(flag)
      members = []
      all_methods(typeinfo_from_ole) do |typeinfo, old_typeinfo, desc, docs, i|
        if desc.invkind & flag
          members << WIN32OLE_METHOD.new(nil, typeinfo, old_typeinfo, desc, docs, i)
        end
        nil
      end
      members
    end

    def all_methods(typeinfo, &block) # MRI: olemethod_from_typeinfo
      return unless typeinfo # Not all ole servers have this info

      # Find method in this type.
      ret = find_all_methods_in(nil, typeinfo, &block)
      return ret if ret

      # Now check all other type impls
      typeinfo.impl_types_count.times do |i|
        begin
          href = typeinfo.get_ref_type_of_impl_type(i)
          ref_typeinfo = typeinfo.get_ref_type_info(href)
          ret = find_all_methods_in(typeinfo, ref_typeinfo, &block)
          return ret if ret
        rescue ComFailException => e
          puts "Error getting impl types #{e}"
        end
      end
      nil
    end

    # MRI: ole_method_sub
    def find_all_methods_in(old_typeinfo, typeinfo, &block)
      typeinfo.funcs_count.times do |i|
        begin
          desc = typeinfo.get_func_desc(i)
          docs = typeinfo.get_documentation(desc.memid)
          ret = yield typeinfo, old_typeinfo, desc, docs, i
          return ret if ret
        rescue ComFailException => e
          puts "Error getting method info #{e}"
        end
      end
      nil
    end

    def typeinfo_from_ole # MRI: typeinfo_from_ole
      typeinfo = type_info
      docs = typeinfo.get_documentation(-1)
      type_lib = typeinfo.get_containing_type_lib
      type_lib.get_type_info_count.times do |i|
        begin
          tdocs = type_lib.get_documentation(i)
          return type_lib.get_type_info(i) if tdocs.name == docs.name
        rescue ComFailException => e
          # We continue on failure. 
        end
      end
      type_info # Actually MRI seems like it could fail in weird case
    end

    def load_typelib(path_reg, arch)
      path = path_reg.open(arch) { |r| r.read(nil) }[1]
#      puts "PATH = #{path}"
      begin
        org.racob.com.Automation.loadTypeLib(path)
      rescue ComFailException => e
#        puts "Failed to load #{name} fom #{path} because: #{e}"
        nil
      end
    end

    def find_all_typeinfo(typelib)
      typelib.type_info_count.times do |i|
        docs = typelib.get_documentation(i)
        next unless docs
        info = typelib.get_type_info(i)
        next unless info
        yield info, docs
      end      
    end

    def all_vars(typeinfo)
      typeinfo.vars_count.times do |i|
        desc = typeinfo.get_var_desc(i)
        next unless desc
        names = typeinfo.get_names(desc.memid)
        next if !names || names.length == 0
        name = names[0]
        next unless name
        yield desc, name
      end      
    end

    def reg_each_key_for(reg, subkey, &block)
      reg.open(subkey) do |subkey_reg|
        subkey_reg.each_key { |key, wtime| block.call(subkey_reg, key) }
      end
    end

    # Walks all guid/clsid entries and yields every single version
    # of those entries to the supplied block. See search_registry as 
    # an example of its usage.
    def typelib_registry_each_guid_version
      Win32::Registry::HKEY_CLASSES_ROOT.open('TypeLib') do |reg| 
        reg.each_key do |guid, wtime|
          reg.open(guid) do |guid_reg|
            guid_reg.each_key do |version_string, wtime|
              version = version_string.to_f
              begin
                guid_reg.open(version_string) do |version_reg|
                  yield guid, version, version_reg
                end
              rescue Win32::Registry::Error => e
                # Version Entry may not contain anything. Skip.
              end
            end
          end
        end
      end
    end

    def registry_subkey(reg, *valid_subkeys)
      reg.each_key do |inner, wtime|
        reg_each_key_for(reg, inner) do |subkey_reg, subkey|
          yield subkey_reg, subkey if valid_subkeys.include? subkey
        end
      end
    end

    def search_registry(typelib_name) # MRI: oletypelib_search_registry
      typelib_registry_each_guid_version do |guid, version, reg|
        name = reg.read(nil)[1] || ''
        registry_subkey(reg, 'win32', 'win64') do |arch_reg, arch|
          type_lib = load_typelib(arch_reg, arch)
#          puts "GUID #{guid} #{version} #{arch} #{type_lib}"
          return type_lib if type_lib && name == typelib_name
        end
      end
      nil
    end

    def typedesc_value(vt, type_details=nil)
      type_string = WIN32OLE::VARIANT.variant_to_string(vt) || "Unknown Type #{vt}"

      type_details << type_string if type_details

      if vt == WIN32OLE::VARIANT::VT_PTR && type_details
        # TODO: Add detail logic
      end

      type_details ? type_details : type_string
    end
  end
end
