require File.dirname(__FILE__) + '/test_dir'
require 'fileutils'

class TestDirWithJarWithoutDirEntry < TestDir # < Test::Unit::TestCase
  JAR = File.expand_path 'jar_with_relative_require1.jar', File.dirname(__FILE__)
  JAR_BAK = JAR + '.bak'

  def setup
    super
    FileUtils.cp JAR, JAR_BAK unless File.exists? JAR_BAK
    `zip #{JAR} -d test/`
  end

  def teardown
    super
    FileUtils.mv JAR_BAK, JAR, :force => true if File.exists? JAR_BAK
  end

end
