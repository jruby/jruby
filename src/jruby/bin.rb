# Provide method aliases to scripts commonly found in ${jruby.home}/bin.

def gem
  require 'jruby/extract'
  JRuby::Extract.new.extract unless File.exist?(Config::CONFIG['bindir'] + "/jruby")
  require 'rubygems'
  Gem.manage_gems
  Gem::GemRunner.new.run(ARGV)
end

def jirb
  require 'irb'
  IRB.start(__FILE__)
end
class << self; alias_method :irb, :jirb; end
