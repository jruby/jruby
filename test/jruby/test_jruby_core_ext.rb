require 'test/unit'

class TestJRubyCoreExt < Test::Unit::TestCase

  def setup
    require 'jruby'; require 'jruby/core_ext'
  end

  def test_jrubyc_inspect_bytecode
    compiled = JRuby.compile script = 'def foo; return :foo end; bar = foo()'
    assert_equal script, compiled.to_s
    assert bytecode = compiled.inspect_bytecode
    assert_equal String, bytecode.class
  end

  def test_string_unseeded_hash; require 'jruby/core_ext/string.rb'
    assert 'foo'.unseeded_hash.is_a?(Integer)
    assert_not_equal '0'.unseeded_hash, ' '.unseeded_hash
    assert_equal '123'.dup.unseeded_hash, "#{123}".unseeded_hash
  end

  def test_string_alloc; require 'jruby/core_ext/string.rb'
    assert String.alloc(128).is_a?(String)
  end

  def test_subclasses
    superclass = Class.new
    sub1 = Class.new(superclass)
    sub2 = Class.new(superclass)
    assert_same_contents [sub1, sub2], superclass.subclasses
  end

  module Some; end
  module Core; end

  class Base < Object
    extend  Some
    include Core
  end

  class User < Base
    include Enumerable
    include Core
  end
  class Role < Base; end
  class SuperUser < User; end

  def test_subclasses_with_modules
    subclasses = Base.subclasses
    assert_equal ['TestJRubyCoreExt::Role', 'TestJRubyCoreExt::User'], subclasses.map(&:name).sort

    assert_same_contents [ Role, User, SuperUser ], Base.subclasses(true)

    assert_same_contents [ SuperUser ], User.subclasses

    SuperUser.extend Module.new
    SuperUser.send :include, Module.new
    assert_equal [ SuperUser ], User.subclasses(true)
    assert_equal [ ], SuperUser.subclasses
    assert_equal [ ], SuperUser.subclasses(true)
    klass = Class.new(SuperUser)
    assert_equal [ klass ], SuperUser.subclasses
    assert_equal [ SuperUser, klass ], User.subclasses(true)

    assert User.to_java.subclasses(true).include? SuperUser
    assert Base.to_java.subclasses # basically that () works
  end

  def test_with_current_runtime_as_global
    other_runtime = org.jruby.Ruby.newInstance
    other_runtime.use_as_global_runtime
    assert_equal other_runtime, org.jruby.Ruby.global_runtime
    JRuby.with_current_runtime_as_global do
      assert_equal JRuby.runtime, org.jruby.Ruby.global_runtime
    end
    assert_equal other_runtime, org.jruby.Ruby.global_runtime
  end

  private

  def assert_same_contents(expect, actual)
    exp = expect.inject({}) { |h, e| h[e] = nil; h }
    act = actual.inject({}) { |h, e| h[e] = nil; h }
    assert_equal exp, act
  end

end
