require 'test/minirunit'
test_check "Test Runtime Callbacks:"

$singleton_method_list = []
$instance_method_list = []

class Foo
  def Foo.singleton_method_added(id); $singleton_method_list << id.id2name; end
  def Foo.method_added(id); $instance_method_list << id.id2name; end
  def self.one; end
  def two; end

  class << self
    def three; end
  end
end

def Foo.four; end

test_equal(["singleton_method_added", "method_added", "one", "three", "four"], $singleton_method_list)
test_equal(["two"], $instance_method_list)

$singleton_method_list = []

obj = "foo"

def obj.singleton_method_added(id)
  $singleton_method_list << id.id2name
end

class << obj
  def one; end
end

def obj.two; end

test_equal(["singleton_method_added", "one", "two"], $singleton_method_list)
