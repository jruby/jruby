
require 'rbyaml'
require 'java'

include_class "org.jruby.yaml.JRubyConstructor"
include_class "org.jruby.util.IOReader"
include_class "org.jvyaml.ComposerImpl"
include_class "org.jvyaml.ParserImpl"
include_class "org.jvyaml.ScannerImpl"
include_class "org.jvyaml.ResolverImpl"

module YAML
  def self.load( io )
    if String === io
      ctor = JRubyConstructor.new(self,ComposerImpl.new(ParserImpl.new(ScannerImpl.new(io)),ResolverImpl.new))
    else
      ctor = JRubyConstructor.new(self,ComposerImpl.new(ParserImpl.new(ScannerImpl.new(IOReader.new(io))),ResolverImpl.new))
    end
    ctor.getData if ctor.checkData
  end

  def self.load_file( filepath )
   File.open( filepath ) do |f|
      load( f )
    end
  end

  # Make YAML module to act exactly as RbYAML
  def self.method_missing(name,*args)
    RbYAML.send(name,*args)
  end

  def self.const_missing(name)
    RbYAML.const_get(name)
  end
end
