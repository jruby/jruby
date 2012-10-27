require File.join(File.expand_path(File.dirname(__FILE__)), 'jruby-jars/version.rb')

module JRubyJars
  PATH = File.expand_path(File.dirname(__FILE__))

  def self.core_jar_path
    PATH + "/jruby-core-#{VERSION}.jar"
  end

  def self.stdlib_jar_path
    PATH + "/jruby-stdlib-#{VERSION}.jar"
  end
end
