require 'java'

class Ant
  def self.get_from_ant
    IO.popen("ant -diagnostics") do |diag|
      home = diag.readlines.grep(/ant.home/).first.sub('ant.home: ', '').chomp
      return home if home
    end
    nil
  end

  def self.locate_ant_home
    return ENV['ANT_HOME'] if ENV['ANT_HOME'] && File.exist?(ENV['ANT_HOME'])
    get_from_ant
  end
  
  ANT_HOME = locate_ant_home
end

# ant-launcher.jar is required because we use Project.init()
$CLASSPATH << File.join(Ant::ANT_HOME, 'lib', 'ant.jar')
$CLASSPATH << File.join(Ant::ANT_HOME, 'lib', 'ant-launcher.jar')

require 'ant/ant'
require 'ant/rake' if defined?(::Rake)
