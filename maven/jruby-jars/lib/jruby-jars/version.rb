module JRubyJars
  MAVEN_VERSION = Dir[File.expand_path(File.dirname(File.dirname(__FILE__))) + '/jruby-core-complete-*jar' ].
      first.gsub(/^.*jruby-core-complete-|.jar$/, '')
  VERSION = MAVEN_VERSION.sub('-SNAPSHOT', '.dev').gsub('-', '.')
end
