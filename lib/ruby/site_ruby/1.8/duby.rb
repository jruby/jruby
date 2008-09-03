require 'duby/transform'
require 'duby/ast'
require 'duby/typer'
require 'duby/compiler'
require 'duby/jvm_compiler'
Dir.open(File.dirname(__FILE__) + "/duby/plugin").each {|file| require "duby/plugin/#{file}" if file =~ /\.rb$/}
require 'jruby'

module Duby
  def self.run(filename, *args)
    java.lang.System.set_property("jruby.duby.enabled", "true")
    
    if filename == '-e'
      filename = 'dash_e'
      ast = Duby::AST.parse(args[0])
    else
      ast = Duby::AST.parse(File.read(filename))
    end

    typer = Duby::Typer::Simple.new(:script)
    ast.infer(typer)
    typer.resolve(true)

    compiler = Duby::Compiler::JVM.new(filename)
    ast.compile(compiler)

    compiler.generate {|filename, builder|
      bytes = builder.generate
      cls = JRuby.runtime.jruby_class_loader.define_class(builder.class_name.gsub(/\//, '.'), bytes.to_java_bytes)
      main_method = cls.get_method("main", [java.lang.String[].java_class].to_java(java.lang.Class))
      main_method.invoke(nil, [args.to_java(:string)].to_java)
    }
  end
end

if __FILE__ == $0
  Duby.run(ARGV[0], *ARGV[1..-1])
end