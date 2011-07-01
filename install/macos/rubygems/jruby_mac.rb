require 'rubygems/config_file'

module Gem

  def self.default_dir
    File.join '/Library/Frameworks/JRuby.framework/Gems', ConfigMap[:ruby_version]
  end

end
