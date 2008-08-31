require 'duby/transform'
require 'duby/ast'
require 'duby/typer'
require 'duby/compiler'
require 'duby/jvm_compiler'
Dir.open(File.dirname(__FILE__) + "/duby/plugin").each {|file| require "duby/plugin/#{file}" if file =~ /\.rb$/}
require 'jruby'

if __FILE__ == $0
  java.lang.System.set_property("jruby.duby.enabled", "true")
  ast = Duby::AST.parse(File.read(ARGV[0]))
  
  ast = Duby::AST.parse(File.read(ARGV[0]))
  
  typer = Duby::Typer::Simple.new(:script)
  ast.infer(typer)
  typer.resolve(true)
  
  compiler = Duby::Compiler::JVM.new(ARGV[0])
  ast.compile(compiler)
  
  compiler.generate {|filename, builder|
    bytes = builder.generate
    cls = JRuby.runtime.jruby_class_loader.define_class(builder.class_name, bytes.to_java_bytes)
    main_method = cls.get_method("main", [java.lang.String[].java_class].to_java(java.lang.Class))
    main_method.invoke(nil, [ARGV[1..-1].to_java(:string)].to_java)
  }
end