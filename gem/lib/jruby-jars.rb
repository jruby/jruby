module JRubyJars
  PATH = File.expand_path(File.dirname(__FILE__))

  def self.jruby_jar_path
    PATH + "/jruby-#{JRUBY_VERSION}.jar"
  end

  def self.stdlib_jar_path
    PATH + "/jruby-stdlib-#{JRUBY_VERSION}.jar"
  end
end
