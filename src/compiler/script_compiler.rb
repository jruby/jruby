
require 'compiling_visitor.rb'

module JavaIO
  include_package 'java.io'
end

module JRuby
  include_package 'org.jruby'
end

class JavaCompiler

  def initialize(script)
    @script = script
  end

  def compile
    class_name = File.basename(@script, '.rb')
    source_name = File.basename(@script)

    java_stream = JavaIO::FileInputStream.new(@script)
    java_reader = JavaIO::InputStreamReader.new(java_stream)

    tree = JRuby::Ruby.getDefaultInstance.parse(java_reader,
                                                source_name)
    code = JRuby::Compiler::CompilingVisitor.new.compile(tree)


    classgen = BCEL::ClassGen.new("jruby." + class_name,
                                  "java.lang.Object",
                                  source_name,
                                  BCEL::Constants::ACC_PUBLIC,
                                  JavaLang::JString[].new(0))
    code.jvm_compile(classgen, "__ruby_main")
    classgen.getJavaClass.dump(class_name + ".rbjvm")
  end
end

if $0 == __FILE__
  compiler = JavaCompiler.new(ARGV[0])
  compiler.compile
end
