# This file is used by JRuby's build to override the RubyGems platform

module RbConfig
    build_props = {}
    File.open(File.join(__FILE__, "../../../../../../default.build.properties")) do |file|
      build_props = java.util.Properties.new
      build_props.load(file.to_input_stream)
    end

    CONFIG = {}
    CONFIG['ruby_version'] = build_props["version.ruby.major"] + '.0'
    CONFIG['arch'] = 'universal-java'
end