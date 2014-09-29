require 'jruby-jars/version'

module JRubyJars
  PATH = File.expand_path(File.dirname(__FILE__))

  def self.core_jar_path
    "#{PATH}/jruby-core-#{JRubyJars::MAVEN_VERSION}.jar"
  end

  def self.stdlib_jar_path
    "#{PATH }/jruby-stdlib-#{JRubyJars::MAVEN_VERSION}.jar"
  end
end
