require 'jruby/core_ext'

describe "A child class with the same fully-qualified name as a parent class" do
  it "uses its reified parent class as its own reified class" do
    module GH1229
      class Foo; end
    end

    foo = GH1229::Foo
    GH1229.send :remove_const, :Foo
    GH1229.const_set :Foo, Class.new(foo)

    GH1229::Foo.become_java!

    foo_ref = JRuby.reference(foo)
    foo2_ref = JRuby.reference(GH1229::Foo)

    expect(foo2_ref.reified_class).to equal(foo_ref.reified_class)
  end
end
