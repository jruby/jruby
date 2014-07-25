require 'jar_installer'
Gem.post_install do |gem_installer|
  Jars::JarInstaller.new( gem_installer.spec ).vendor_jars unless (ENV['JRUBY_SKIP_JAR_DEPENDENCIES'] || java.lang.System.get_property('jruby.skip.jar.dependencies')) == 'true'
end
