class BeanExtender
  include_class 'java.beans.Introspector'
  def extend_proxy(proxy_class, java_class)
     property_descriptors = Introspector.getBeanInfo(java_class).getPropertyDescriptors
     property_descriptors.each {|pd|
       define_attr_reader(proxy_class, pd)
       define_attr_writer(proxy_class, pd)
     }
  end
  
  def define_alias proxy_class, method, aliasName
    return if proxy_class.instance_methods.member?(aliasName) || method == nil
    proxy_class.class_eval {
      alias_method aliasName, method.getName
    }    
  end

  def define_attr_reader(proxy_class, pd)
    return if !pd.getReadMethod
    
    define_alias(proxy_class, pd.getReadMethod, pd.getName)
    if pd.getPropertyType.to_s == 'boolean'
      define_alias(proxy_class, pd.getReadMethod, pd.getName+"?")
    end
  end
  
  def define_attr_writer(proxy_class, pd)
    return if !pd.getWriteMethod
    define_alias proxy_class, pd.getWriteMethod, "#{pd.getName}="
  end
  
  def extend_method(proxy_class, name, methods)
    if name =~ /^([gs]et|is)([A-Z0-9][a-zA-Z0-9_]*)/
      action, short_name = $1, $2[0,1].downcase + $2[1..-1]

	  case action
      when "get", "is"
        proxy_class.class_eval { alias_method short_name, name }
        if (methods.first.return_type.to_s == "boolean")
          proxy_class.class_eval { alias_method short_name + "?", name }
        end
	  when "set"
        proxy_class.class_eval { alias_method short_name + "=", name }
      end
    end
  end
end

bean_extender = BeanExtender.new
# Since we lazily create methods now we are not using the proxy extender portion of this currently
#JavaUtilities.add_proxy_extender bean_extender
JavaProxy.add_method_extender bean_extender
