# this is a dummy file for those apps (like RubyGems) that explicitly require 'yaml/syck'

module YAML
  module Syck
    Map = ::YAML::JvYAML::Map
    Seq = ::YAML::JvYAML::Seq
    Scalar = ::YAML::JvYAML::Scalar
  end
end
