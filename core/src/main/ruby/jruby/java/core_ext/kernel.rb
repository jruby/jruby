# frozen-literal-string: true

# Convenience methods for top-level packages without the need to prefix e.g. `Java::java.util.ArrayList`.
# @note These methods are undef-ed within Java package stubs (in case of *com.foo.com*).
module Kernel
  # Java package short-cut method.
  # @example
  #    java.lang.System
  def java
    JavaUtilities.get_package_module_dot_format('java') # stub
  end
  # Java package short-cut method.
  # @example
  #    javax.swing.SwingUtilities
  def javax
    JavaUtilities.get_package_module_dot_format('javax') # stub
  end
  # Java package short-cut method.
  # @example
  #    javafx.application.Platform
  def javafx
    JavaUtilities.get_package_module_dot_format('javafx') # stub
  end
  # Java package short-cut method.
  # @example
  #    com.google.common.base.Strings
  def com
    JavaUtilities.get_package_module_dot_format('com') # stub
  end
  # Java package short-cut method.
  # @example
  #    org.json.JSONArray
  def org
    JavaUtilities.get_package_module_dot_format('org') # stub
  end
end if false # only here for doc -> implementation at org.jruby.javasupport.ext.Kernel
