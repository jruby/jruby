require_relative "../../java_integration/spec_helper"
require 'jruby'
require 'jruby/compiler'

describe "A Ruby class generating a Java stub" do
  def generate(script)
    node = JRuby.parse(script)
    # we use __FILE__ so there's something for it to read
    JRuby::Compiler::JavaGenerator.generate_java node, __FILE__
  end

  describe "with additional java_import lines" do
    it "generates imports into the java source" do
      script = generate("java_import 'org.foo.Bar'; class Foo; end")

      script.imports.length.should > 1
      script.imports.should include "org.foo.Bar"

      java = script.to_s
      java.should match /import org\.foo\.Bar;/

      script = generate("java_import 'org.foo.Bar', 'org.foo.Baz'; class Foo; end")

      script.imports.length.should > 2
      script.imports.should include "org.foo.Bar"
      script.imports.should include "org.foo.Baz"

      java = script.to_s
      java.should match /import org\.foo\.Bar;/
      java.should match /import org\.foo\.Baz;/
    end
  end
end