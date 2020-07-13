require_relative '../spec_helper'

describe "A Ruby class generating a Java stub" do
  def generate(script)
    node = JRuby.parse(script)
    # we use __FILE__ so there's something for it to read
    JRuby::Compiler::JavaGenerator.generate_java node, __FILE__
  end

  describe "with additional java_import lines" do
    it "generates imports into the java source" do
      script = generate("java_import 'org.foo.Bar'; class Foo; end")

      expect( script.imports.length ).to be > 1
      expect( script.imports ).to include "org.foo.Bar"

      java = script.to_s
      expect( java ).to match /import org\.foo\.Bar;/

      script = generate("java_import 'org.foo.Bar', 'org.foo.Baz'; class Foo; end")

      expect( script.imports.length ).to be > 2
      expect( script.imports ).to include "org.foo.Bar"
      expect( script.imports ).to include "org.foo.Baz"

      java = script.to_s
      expect( java ).to match /import org\.foo\.Bar;/
      expect( java ).to match /import org\.foo\.Baz;/
    end
  end
end