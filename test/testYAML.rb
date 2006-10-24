require 'test/minirunit'
require 'yaml'

test_equal("str", YAML.load("!str str"))
test_equal("str", YAML.load("--- str"))
test_equal("str", YAML.load("---\nstr"))
test_equal("str", YAML.load("--- \nstr"))
test_equal("str", YAML.load("--- \n str"))
test_equal("str", YAML.load("str"))
test_equal("str", YAML.load(" str"))
test_equal("str", YAML.load("\nstr"))
test_equal("str", YAML.load("\n str"))
test_equal("str", YAML.load('"str"'))
test_equal("str", YAML.load("'str'"))
test_equal("str", YAML.load(" --- 'str'"))
test_equal("1.0", YAML.load("!str 1.0"))

test_equal(47, YAML.load("47"))
test_equal(0, YAML.load("0"))
test_equal(-1, YAML.load("-1"))

test_equal({'a' => 'b', 'c' => 'd' }, YAML.load("a: b\nc: d"))
test_equal({'a' => 'b', 'c' => 'd' }, YAML.load("c: d\na: b\n"))

test_equal({'a' => 'b', 'c' => 'd' }, YAML.load("{a: b, c: d}"))
test_equal({'a' => 'b', 'c' => 'd' }, YAML.load("{c: d,\na: b}"))

test_equal(%w(a b c), YAML.load("--- \n- a\n- b\n- c\n"))
test_equal(%w(a b c), YAML.load("--- [a, b, c]"))
test_equal(%w(a b c), YAML.load("[a, b, c]"))

test_equal("--- str\n", "str".to_yaml)
test_equal("--- \na: b\n", {'a'=>'b'}.to_yaml)
test_equal("--- \n- a\n- b\n- c\n", %w(a b c).to_yaml)

test_equal("--- !str 1.0\n", "1.0".to_yaml)

class TestBean
  attr_accessor :value, :key
  def initialize(v,k)
    @value=v
    @key=k
  end
  
  def ==(other)
    self.class == other.class && self.value == other.value && self.key == other.key
  end
end

test_equal("--- !ruby/object:TestBean\nvalue: 13\nkey: 42\n", TestBean.new(13,42).to_yaml)
test_equal(TestBean.new(13,42),YAML.load("--- !ruby/object:TestBean \nvalue: 13\nkey: 42\n"))

TestStruct = Struct.new(:foo,:bar)
test_equal("--- !ruby/struct:TestStruct\nfoo: 13\nbar: 42\n", TestStruct.new(13,42).to_yaml)
test_equal("--- !ruby/exception:StandardError\nmessage: foobar\n", StandardError.new("foobar").to_yaml)

test_equal("--- :foo\n", :foo.to_yaml)

test_equal("--- !ruby/range\nbegin: 1\nend: 3\nexcl: false\n", (1..3).to_yaml)
test_equal("--- !ruby/range\nbegin: 1\nend: 3\nexcl: true\n", (1...3).to_yaml)

test_equal("--- !ruby/regexp /^abc/\n", /^abc/.to_yaml)

test_equal("--- 1982-05-03 15:32:44 Z\n",Time.utc(1982,05,03,15,32,44).to_yaml)
test_equal("--- 2005-05-03\n",Date.new(2005,5,3).to_yaml)

test_equal("--- .NaN\n",(0.0/0.0).to_yaml)
test_equal("--- .Inf\n",(1.0/0.0).to_yaml)
test_equal("--- -.Inf\n",(-1.0/0.0).to_yaml)
test_equal("--- 0.0\n", (0.0).to_yaml)
test_equal("--- 0\n", 0.to_yaml)

test_equal("--- true\n", true.to_yaml)
test_equal("--- false\n", false.to_yaml)

test_equal("--- \n", nil.to_yaml)

puts
