require 'jruby/compiler/java_class'

describe JRuby::Compiler::RubyScript do

  it "resets import list for each instance" do
    script1 = JRuby::Compiler::RubyScript.new("script1")
    script1.add_import('my.java.import')

    script2 = JRuby::Compiler::RubyScript.new("script2")
    script2.imports.should_not include('my.java.import')
  end

end
