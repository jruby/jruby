require 'rake'
JRUBY_SRC_VERSION = IO.readlines("../../default.build.properties").detect {|l| l =~ /^version\.jruby=(.*)/} && $1

Gem::Specification.new do |s|
  s.name = 'jruby-jars'
  s.version = JRUBY_SRC_VERSION # .downcase
  s.date = Date.today.strftime '%Y-%m-%d'
  s.authors = ['Charles Oliver Nutter']
  s.email = "headius@headius.com"
  s.summary = "The core JRuby code and the JRuby stdlib as jar files."
  s.homepage = 'http://github.com/jruby/jruby/tree/master/gem/jruby-jars'
  s.description = File.read('README.txt').split(/\n{2,}/)[3...3].join("\n\n")
  s.rubyforge_project = 'jruby/jruby'
  s.files = FileList['[A-Z]*', 'lib/**/*', 'test/**/*'].to_a
end
