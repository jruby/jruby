class BeanExtender
  def extend_proxy(proxy_class, java_class)
     public_methods = java_class.java_instance_methods.select { |m| m.public? }
     public_methods = public_methods.group_by {|m| m.name}
     public_methods.each {|name, methods|
	      # Lets treat javabean properties like ruby attributes
       case name[0,4]
	     when /get./
	       newName = name[3..-1].downcase!
             next if proxy_class.instance_methods.member?(newName)
	         proxy_class.class_eval {
	             define_method(newName) do |*args|
                   args = convert_arguments(args)
                   m = JavaUtilities.matching_method(methods.find_all {|m| m.arity == args.length}, args)
                   result = m.invoke(self.java_object, *args)
                   result = Java.java_to_primitive(result)
                   if result.kind_of?(JavaObject)
                     result = JavaUtilities.wrap(result, m.return_type)
                   end
                   result
                 end
              }
	      when /set./
	      	  proxy_class.class_eval {
	      	    define_method(name[3..-1].downcase!.concat('=')) do|*args|
                  args = convert_arguments(args)
                  m = JavaUtilities.matching_method(methods.find_all {|m| m.arity == args.length}, args)
                  result = m.invoke(self.java_object, *args)
                  result = Java.java_to_primitive(result)
                  if result.kind_of?(JavaObject)
                    result = JavaUtilities.wrap(result, m.return_type)
                  end
                  result
                end
	          }
	      end
      }
    end
 end
JavaUtilities.add_proxy_extender BeanExtender.new
