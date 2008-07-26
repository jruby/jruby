include Java
module JFFI
  module Platform
    LONG_SIZE = Java::org.jruby.ext.ffi.Platform.getPlatform.long_size
    ADDRESS_SIZE = Java::org.jruby.ext.ffi.Platform.getPlatform.address_size
    IS_32_BIT = 32 == ADDRESS_SIZE
    IS_64_BIT = 64 == ADDRESS_SIZE

    # An extension over <code>System.getProperty</code> method.
    # Handles security restrictions, and returns the default
    # value if the access to the property is restricted.
    def self.get_property(property, def_value = nil)
      begin
        java.lang.System.getProperty(property, def_value);
      rescue java.lang.SecurityException
        def_value
      end
    end
    OS_NAME = get_property("os.name", "unknown")
    OS_NAME_LC = OS_NAME.downcase
    # Generic Windows designation
    WINDOWS = "windows"
  
    WINDOWS_NT = "nt"
    WINDOWS_20X = "windows 2"
    WINDOWS_XP = "windows xp"
    WINDOWS_VISTA = "vista"
    MAC_OS = "mac os"
    DARWIN = "darwin"
    FREEBSD = "freebsd"
    OPENBSD = "openbsd"
    LINUX = "linux"
    SOLARIS = "sunos"
    IS_WINDOWS = OS_NAME_LC.include?(WINDOWS)
    IS_WINDOWS_NT = IS_WINDOWS && OS_NAME_LC.include?(WINDOWS_NT)
    IS_WINDOWS_20X = OS_NAME_LC.include?(WINDOWS_20X)
    IS_WINDOWS_XP = OS_NAME_LC.include?(WINDOWS_XP)
    IS_WINDOWS_VISTA = IS_WINDOWS && OS_NAME_LC.include?(WINDOWS_VISTA)
    IS_MAC = OS_NAME_LC.index(MAC_OS) == 0 || OS_NAME_LC.index(DARWIN) == 0
    IS_FREEBSD = OS_NAME_LC.index(FREEBSD) == 0
    IS_OPENBSD = OS_NAME_LC.index(OPENBSD) == 0
    IS_LINUX = OS_NAME_LC.index(LINUX) == 0
    IS_SOLARIS = OS_NAME_LC.index(SOLARIS) == 0
    IS_BSD = IS_MAC || IS_FREEBSD || IS_OPENBSD
    ARCH = java.lang.System.getProperty("os.arch")
    NAME = if IS_WINDOWS
      "#{ARCH}-windows"
    elsif IS_MAC
      "#{ARCH}-darwin"
    elsif IS_FREEBSD
      "#{ARCH}-freebsd"
    elsif IS_FREEBSD
      "#{ARCH}-openbsd"
    elsif IS_LINUX
      "#{ARCH}-linux"
    else
      "#{ARCH}-unknown"
    end
    FFI_DIR = File.dirname(__FILE__)
    CONF_DIR = File.join(File.dirname(__FILE__), "platform", NAME)
  end
end

