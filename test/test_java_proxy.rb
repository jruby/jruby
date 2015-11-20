require 'test/unit'

class TestJavaProxy < Test::Unit::TestCase

  require 'java'

  SampleInterface = org.jruby.javasupport.test.SampleInterface

  class RubyImpl
    include SampleInterface
    #  public interface SampleInterface {
    #      Integer intMethod(Integer i);
    #      String oneArgument(String s);
    #      String noArguments();
    #  }
    def intMethod(i); i + 1 end
    def oneArgument(s); s.dup end
    def noArguments; "Hello" end
  end

  class ProxyHandler
    include java.lang.reflect.InvocationHandler

    def initialize
      @impl = RubyImpl.new
    end

    # invoke(Object proxy, Method method, Object[] args)
    def invoke(proxy, method, args)
      method.invoke(@impl, *args)
      # with non splatted args noArgument would fail as an empty array gets passed
    end
  end

  java_import java.lang.reflect.Proxy

  def setup
    proxy = ProxyHandler.new
    interface = SampleInterface
    loader = interface.java_class.class_loader
    ifaces = [ interface.java_class ].to_java(java.lang.Class)
    @proxy = Proxy.newProxyInstance(loader, ifaces, proxy)
  end

  def test_one_argument_method
    assert_equal("Howdy", @proxy.oneArgument("Howdy"))
  end

  def test_int_method
    # ClassCastException: java.lang.Long cannot be cast to java.lang.Integer
    # IllegalArgumentException: argument type mismatch
    # NOTE: not working due obviously converting to long implicitly :
    # assert_equal(2, @proxy.intMethod(1))
    assert_equal(2, @proxy.intMethod(1.to_java(:int)))
  end

  def test_no_arguments_method
    assert_equal("Hello", @proxy.noArguments)
  end

end
