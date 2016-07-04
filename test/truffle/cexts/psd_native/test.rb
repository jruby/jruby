require 'psd'
require 'psd_native'

module PSDNative::Util
  extend self
end

abort unless PSDNative::Util.clamp(40, 10, 90) == PSD::Util.clamp(40, 10, 90)
abort unless PSDNative::Util.clamp(5, 10, 90) == PSD::Util.clamp(5, 10, 90)
abort unless PSDNative::Util.clamp(100, 10, 90) == PSD::Util.clamp(100, 10, 90)
