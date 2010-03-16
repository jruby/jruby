require 'java'

class Ant
  def self.load_from_ant
    IO.popen("ant -diagnostics") do |diag|
      classpath_jars = []
      listing_path = nil
      jar_path = nil
      diag.readlines.each do |line|
        if line =~ /^ant\.home: (.*)$/ && !defined?(ANT_HOME)
          const_set(:ANT_HOME, $1)
        elsif line =~ /Tasks availability/
          break
        elsif line =~ /^ (.*) jar listing$/
          listing_path = $1
        elsif line =~ /^(.*\.home): (.*)$/
          home_var, path = $1, $2
          jar_path = listing_path.sub(home_var.upcase.sub('.','_'), path)
        elsif line =~ /^ant\.core\.lib: (.*)$/
          classpath_jars << $1
        elsif line =~ /^(.*\.jar) \(\d+ bytes\)/
          classpath_jars << File.join(jar_path, $1)
        end
      end
      classpath_jars.uniq.each {|j| $CLASSPATH << j }
    end
  end

  def self.load
    if ENV['ANT_HOME'] && File.exist?(ENV['ANT_HOME'])
      const_set(:ANT_HOME, ENV['ANT_HOME'])
      Dir["#{ANT_HOME}/lib/*.jar"].each {|j| $CLASSPATH << j }
    else
      load_from_ant
    end
  end
  load
end

require 'ant/ant'
require 'ant/rake' if defined?(::Rake)
