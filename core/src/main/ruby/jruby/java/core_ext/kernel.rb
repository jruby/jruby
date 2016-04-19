# frozen-literal-string: true

# Create convenience methods for top-level java packages so we do not need to prefix
# with Java::com. We undef these methods within Package in case we run into 'com.foo.com'.
module Kernel
  def java
    JavaUtilities.get_package_module_dot_format('java')
  end

  def javax
    JavaUtilities.get_package_module_dot_format('javax')
  end

  def javafx
    JavaUtilities.get_package_module_dot_format('javafx')
  end

  def com
    JavaUtilities.get_package_module_dot_format('com')
  end

  def org
    JavaUtilities.get_package_module_dot_format('org')
  end
end
