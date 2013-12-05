require 'rake'
require 'rexml/document'
require 'rexml/xpath'

doc = REXML::Document.new File.new(File.join('..', '..', 'pom.xml'))
version = REXML::XPath.first(doc, "//project/version").text

Gem::Specification.new do |s|
  s.name = 'jruby-jars'
  s.version = version.sub( /-SNAPSHOT/, '.SNAPSHOT' )
  s.authors = ['Charles Oliver Nutter']
  s.email = "headius@headius.com"
  s.summary = "The core JRuby code and the JRuby stdlib as jar files."
  s.homepage = 'http://github.com/jruby/jruby/tree/master/gem/jruby-jars'
  s.description = File.read('README.txt', encoding: "UTF-8").split(/\n{2,}/)[3]
  s.rubyforge_project = 'jruby/jruby'
  s.files = FileList['[A-Z]*', 'lib/**/*.rb', "lib/**/jruby-*-#{version}.jar", 'test/**/*'].to_a
end
