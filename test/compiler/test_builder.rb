require 'test/unit'
require 'compiler/builder'

class TestBuilder < Test::Unit::TestCase
  import java.util.ArrayList

  def test_file_builder
    builder = Compiler::FileBuilder.build("somefile.source") do
      package "org.awesome", "stuff" do
        public_class "MyClass", object do
          public_field "list", ArrayList

          public_constructor string, ArrayList do
            aload 0
            invokespecial object, "<init>", [void]
            aload 0
            aload 1
            aload 2
            invokevirtual this, "bar", [ArrayList, string, ArrayList]
            aload 0
            swap
            putfield this, "list", ArrayList
            returnvoid
          end

          public_static_method "foo", this, string do
            new this
            dup
            aload 0
            new ArrayList
            dup
            invokespecial ArrayList, "<init>", [void]
            invokespecial this, "<init>", [void, string, ArrayList]
            areturn
          end

          public_method "bar", ArrayList, string, ArrayList do
            aload 1
            invokevirtual(string, "toLowerCase", string)
            aload 2
            swap
            invokevirtual(ArrayList, "add", [boolean, object])
            aload 2
            areturn
          end

          public_method("getList", ArrayList) do
            aload 0
            getfield this, "list", ArrayList
            areturn
          end

          public_static_method("main", void, string[]) do
            aload 0
            ldc_int 0
            aaload
            invokestatic this, "foo", [this, string]
            invokevirtual this, "getList", ArrayList
            aprintln
            returnvoid
          end
        end
      end
    end

    # admittedly, this isn't much of a unit test, but it is what it is
    assert builder
  end
end