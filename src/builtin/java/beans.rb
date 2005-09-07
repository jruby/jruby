class BeanExtender
  include_class 'java.beans.Introspector'
  def extend_proxy(proxy_class)
     Introspector.getBeanInfo(proxy_class.java_class).getPropertyDescriptors.each do |pd|
       define_attr_reader(proxy_class, pd) if pd.getReadMethod
       define_attr_writer(proxy_class, pd) if pd.getWriteMethod
     end
  end
  
  def define_alias proxy_class, method, aliasName
    return if proxy_class.instance_methods.member?(aliasName) || method == nil
    proxy_class.class_eval {
      alias_method aliasName, method.getName
    }    
  end

  def define_attr_reader(proxy_class, pd)
    define_alias(proxy_class, pd.getReadMethod, pd.getName)
    if pd.getPropertyType.to_s == 'boolean'
      define_alias(proxy_class, pd.getReadMethod, pd.getName+"?")
    end
  end
  
  def define_attr_writer(proxy_class, pd)
    define_alias proxy_class, pd.getWriteMethod, "#{pd.getName}="
  end
  
 end
JavaUtilities.add_proxy_extender BeanExtender.new
