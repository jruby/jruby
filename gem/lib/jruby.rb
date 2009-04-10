module JRuby
  PATH = File.dirname(__FILE__)

  def self.jruby_jar_path
    PATH + "jruby.jar"
  end

  def self.stdlib_jar_path
    PATH + "jruby-stdlib.jar"
  end
end
