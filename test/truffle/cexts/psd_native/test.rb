require 'psd_native'

module PSDNative::Util
  extend self
end

abort unless PSDNative::Util.clamp(40, 10, 90) == 40
abort unless PSDNative::Util.clamp(5, 10, 90) == 10
abort unless PSDNative::Util.clamp(100, 10, 90) == 90
