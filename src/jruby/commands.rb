require 'rbconfig'

# Provide method aliases to scripts commonly found in ${jruby.home}/bin.

if File.directory?(Config::CONFIG['bindir'])
  Dir[Config::CONFIG['bindir'] + '/*'].each do |f|
    if File.file?(f) && File.open(f) {|io| (io.readline rescue "") =~ /^#!.*ruby/}
      meth = File.basename(f)
      mod = class << self; self; end
      mod.send(:define_method, meth) do
        require 'jruby/extract'
        JRuby::Extract.new.extract unless File.exist?(Config::CONFIG['bindir'] + "/jruby")
        load f
      end
    end
  end
else
  # allow use of 'gem' and 'jirb' without prior extraction
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
end

def extract
  require 'jruby/extract'
  JRuby::Extract.new(ARGV.first).extract
end

class << self; alias_method :irb, :jirb; end
