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
test_equal(:str, YAML.load(":str"))

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

test_ok(["--- !ruby/object:TestBean\nvalue: 13\nkey: 42\n",
         "--- !ruby/object:TestBean\nkey: 42\nvalue: 13\n"].include?(TestBean.new(13,42).to_yaml))
test_equal(TestBean.new(13,42),YAML.load("--- !ruby/object:TestBean \nvalue: 13\nkey: 42\n"))

TestStruct = Struct.new(:foo,:bar)
test_ok(["--- !ruby/struct:TestStruct\nfoo: 13\nbar: 42\n","--- !ruby/struct:TestStruct\nbar: 42\nfoo: 13\n"].include?(TestStruct.new(13,42).to_yaml))
test_equal("--- !ruby/exception:StandardError\nmessage: foobar\n", StandardError.new("foobar").to_yaml)

test_equal("--- :foo\n", :foo.to_yaml)

test_ok(["--- !ruby/range\nbegin: 1\nend: 3\nexcl: false\n",
         "--- !ruby/range\nbegin: 1\nexcl: false\nend: 3\n",
         "--- !ruby/range\nend: 3\nbegin: 1\nexcl: false\n",
         "--- !ruby/range\nend: 3\nexcl: false\nbegin: 1\n",
         "--- !ruby/range\nexcl: false\nbegin: 1\nend: 3\n",
         "--- !ruby/range\nexcl: false\nend: 3\nbegin: 1\n"].include?((1..3).to_yaml))
test_ok(["--- !ruby/range\nbegin: 1\nend: 3\nexcl: true\n",
         "--- !ruby/range\nbegin: 1\nexcl: true\nend: 3\n",
         "--- !ruby/range\nend: 3\nbegin: 1\nexcl: true\n",
         "--- !ruby/range\nend: 3\ntrue: false\nbegin: 1\n",
         "--- !ruby/range\nexcl: true\nbegin: 1\nend: 3\n",
         "--- !ruby/range\nexcl: true\nend: 3\nbegin: 1\n"].include?((1...3).to_yaml))

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

test_equal("--- :foo\n", :foo.to_yaml)

# JRUBY-718
test_equal("--- \"\"\n", ''.to_yaml)
test_equal('', YAML.load("---\n!str"))

# JRUBY-719
test_equal('---', YAML.load("--- ---\n"))
test_equal('---', YAML.load("---"))

astr = "abcde"
shared = astr[2..-1]
test_equal('cde', YAML.load(shared))
test_equal("--- cde\n", shared.to_yaml)

# JRUBY-1026
a = "one0.1"
b = a[3..-1]
test_equal("--- !str 0.1\n", YAML.dump(b))

# JRUBY-1169
class HashWithIndifferentAccess < Hash
end

hash = HashWithIndifferentAccess.new
hash['kind'] = 'human'
need_to_be_serialized = {:first => 'something', :second_params => hash}
a = {:x => need_to_be_serialized.to_yaml}
test_equal need_to_be_serialized, YAML.load(YAML.load(a.to_yaml)[:x])

# JRUBY-1220 - make sure all three variations work
bad_text = " A\nR"
dump = YAML.dump({'text' => bad_text})
loaded = YAML.load(dump)
test_equal bad_text, loaded['text']

bad_text = %{
 A
R}
dump = YAML.dump({'text' => bad_text})
loaded = YAML.load(dump)
test_equal bad_text, loaded['text']

bad_text = %{
 ActiveRecord::StatementInvalid in ProjectsController#confirm_delete
RuntimeError: ERROR	C23503	Mupdate or delete on "projects" violates foreign 
    }
dump = YAML.dump({'text' => bad_text})
loaded = YAML.load(dump)
test_equal bad_text, loaded['text']

string = <<-YAML
outer
  property1: value1
  additional:
  - property2: value2
    color: green
    data: SELECT 'xxxxxxxxxxxxxxxxxxx', COUNT(*) WHERE xyzabc = 'unk'
    combine: overlay-bottom
YAML
test_equal string, YAML.load(YAML.dump(string))

## TODO: implement real fuzz testing of YAML round tripping here
