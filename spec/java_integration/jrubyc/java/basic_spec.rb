require File.dirname(__FILE__) + "/../../spec_helper"
require 'rbconfig'
require 'jruby'
require 'jruby/compiler'

describe "A Ruby class generating a Java stub" do
  def generate(script)
    node = JRuby.parse(script)
    # we use __FILE__ so there's something for it to read
    JRuby::Compiler::JavaGenerator.generate_java node, __FILE__
  end
    
  it "generates Java source" do
    script = generate("class Foo; end")
    script.should_not == nil
    script.classes[0].name.should == "Foo"
    java = script.classes[0].to_s

    # a few sanity checks for default behaviors
    java.should match /import org\.jruby\.Ruby;/
    java.should match /public class Foo {/
    java.should match /static {/
    java.should match /public Foo\(Ruby \w+, RubyClass \w+\)/
  end

  it "generates a javac command" do
    files = %w[Foo.java Bar.java]
    javac = JRuby::Compiler::JavaGenerator.generate_javac(
      files,
      ENV_JAVA['java.class.path'].split(RbConfig::CONFIG['PATH_SEPARATOR']),
      '/tmp')

    javac.should match /javac/
    javac.should match /jruby\w*\.jar/
    javac.should match /Foo\.java/
    javac.should match /Bar\.java/
  end
end
