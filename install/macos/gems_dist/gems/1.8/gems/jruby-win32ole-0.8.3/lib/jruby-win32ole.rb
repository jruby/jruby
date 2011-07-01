require 'java'

# Set location so the loader can find the native dll it needs
arch = ENV_JAVA['sun.arch.data.model'] == '32' ? 'x86' : 'x64'
java.lang.System.set_property 'racob.dll.path',
  File.dirname(__FILE__) + "/racob-#{arch}.dll"

require 'racob.jar'

require 'win32ole/win32ole'      # <- java native impl of WIN32OLE
require 'win32ole/win32ole_ruby' # <- ruby impl of WIN32OLE

java_import org.racob.com.Variant # Needed for static native initializer :(

java_import org.jruby.ext.win32ole.RubyWIN32OLE
java_import org.jruby.ext.win32ole.RubyInvocationProxy

require 'win32ole/win32ole_error'
require 'win32ole/win32ole_method'
require 'win32ole/win32ole_variant'
require 'win32ole/win32ole_variable'
require 'win32ole/win32ole_event'
require 'win32ole/win32ole_param'
require 'win32ole/win32ole_type'
require 'win32ole/win32ole_typelib'
