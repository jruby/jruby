require 'jruby-jars/version'

module JRubyJars
  PATH = File.expand_path(File.dirname(__FILE__))

  def self.core_jar_path
    Dir[ PATH + "/jruby-core-complete-*.jar" ].first
  end

  def self.stdlib_jar_path
    Dir[ PATH + "/jruby-stdlib-complete-*.jar" ].first
  end
end
