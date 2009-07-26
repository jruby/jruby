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

test_equal("--- \"1.0\"\n", "1.0".to_yaml)

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

test_ok(["--- !ruby/object:TestBean \nvalue: 13\nkey: 42\n",
         "--- !ruby/object:TestBean \nkey: 42\nvalue: 13\n"].include?(TestBean.new(13,42).to_yaml))
test_equal(TestBean.new(13,42),YAML.load("--- !ruby/object:TestBean \nvalue: 13\nkey: 42\n"))

TestStruct = Struct.new(:foo,:bar)
test_ok(["--- !ruby/struct:TestStruct \nfoo: 13\nbar: 42\n","--- !ruby/struct:TestStruct \nbar: 42\nfoo: 13\n"].include?(TestStruct.new(13,42).to_yaml))
test_equal("--- !ruby/exception:StandardError \nmessage: foobar\n", StandardError.new("foobar").to_yaml)

test_equal("--- :foo\n", :foo.to_yaml)

test_equal(["--- !ruby/range ", "begin: 1", "end: 3", "excl: false"], (1..3).to_yaml.split("\n").sort)
test_equal(["--- !ruby/range ", "begin: 1", "end: 3", "excl: true"], (1...3).to_yaml.split("\n").sort)

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
test_equal("--- \"0.1\"\n", YAML.dump(b))

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

text = " "*80 + "\n" + " "*30
test_equal text, YAML.load(YAML.dump(text))

text = <<-YAML
  - label: New
    color: green
    data: SELECT 'Iteration Scheduled', COUNT(*) WHERE Status = 'New'
    combine: overlay-bottom
  - label: Open
    color: pink
    data: SELECT 'Iteration Scheduled', COUNT(*) WHERE Status = 'Open'
    combine: overlay-bottom
  - label: Ready for Development
    color: yellow
    data: SELECT 'Iteration Scheduled', COUNT(*) WHERE Status = 'Ready for Development'
    combine: overlay-bottom
    color: blue
    data: SELECT 'Iteration Scheduled', COUNT(*) WHERE Status = 'Complete'
    combine: overlay-bottom
  - label: Other statuses
    color: red
    data: SELECT 'Iteration Scheduled', COUNT(*)
                    combine: total
YAML

test_equal text, YAML.load(YAML.dump(text))

text = <<-YAML
stack-bar-chart
  conditions: 'Release' in (R1) and not 'Iteration Scheduled' = null
  labels: SELECT DISTINCT 'Iteration Scheduled' ORDER BY 'Iteration Scheduled'
  cumulative: true
  series:
  - label: New
    color: green
    data: SELECT 'Iteration Scheduled', COUNT(*) WHERE Status = 'New'
    combine: overlay-bottom
  - label: Open
    color: pink
    data: SELECT 'Iteration Scheduled', COUNT(*) WHERE Status = 'Open'
    combine: overlay-bottom
  - label: Ready for Development
    color: yellow
    data: SELECT 'Iteration Scheduled', COUNT(*) WHERE Status = 'Ready for Development'
    combine: overlay-bottom
  - label: Complete
    color: blue
    data: SELECT 'Iteration Scheduled', COUNT(*) WHERE Status = 'Complete'
    combine: overlay-bottom
  - label: Other statuses
    color: red
    data: SELECT 'Iteration Scheduled', COUNT(*)
    combine: total
YAML

test_equal text, YAML.load(YAML.dump(text))

text = <<YAML
valid_key:
key1: value
invalid_key
akey: blah
YAML

test_exception(ArgumentError) do 
  YAML.load(text)
end

def roundtrip(text)
  test_equal text, YAML.load(YAML.dump(text))
end

roundtrip("C VW\205\v\321XU\346")
roundtrip("\n8 xwKmjHG")
roundtrip("1jq[\205qIB\ns")
roundtrip("\rj\230fso\304\nEE")
roundtrip("ks]qkYM\2073Un\317\nL\346Yp\204 CKMfFcRDFZ\vMNk\302fQDR<R\v \314QUa\234P\237s aLJnAu \345\262Wqm_W\241\277J\256ILKpPNsMPuok")

def fuzz_roundtrip(str)
  str.gsub! "\n ", "\n"
  out = YAML.load(YAML.dump(str))
  test_equal str, out
end

values = (1..255).to_a
values.delete(13)
more = ('a'..'z').to_a + ('A'..'Z').to_a
blanks = [' ', "\t", "\n"]

types = [more*10 + blanks*2, values + more*10 + blanks*2, values + more*10 + blanks*20]
sizes = [10, 81, 214]

errors = []
types.each do |t|
  sizes.each do |s|
    1000.times do |vv|
      val = ""
      s.times do 
        val << t[rand(t.length)]
      end
      fuzz_roundtrip(val)
    end      
  end
end

test_no_exception do 
  YAML.load_file("test/yaml/does_not_work.yml")
end

roundtrip :"1"


# Fix for JRUBY-1471
class YamlTest
  def initialize
    @test = Hash.new
    @test["hello"] = "foo"
  end
end

list = [YamlTest.new, YamlTest.new, YamlTest.new]
test_equal 3, list.map{ |ll| ll.object_id }.uniq.length
list2 = YAML.load(YAML.dump(list))
test_equal 3, list2.map{ |ll| ll.object_id }.uniq.length

# JRUBY-1659
YAML.load("{a: 2007-01-01 01:12:34}")

# JRUBY-1765
#test_equal Date.new(-1,1,1), YAML.load(Date.new(-1,1,1).to_yaml)

# JRUBY-1766
test_ok YAML.load(Time.now.to_yaml).instance_of?(Time)
test_ok YAML.load("2007-01-01 01:12:34").instance_of?(String)
test_ok YAML.load("2007-01-01 01:12:34.0").instance_of?(String)
test_ok YAML.load("2007-01-01 01:12:34 +00:00").instance_of?(Time)
test_ok YAML.load("2007-01-01 01:12:34.0 +00:00").instance_of?(Time)
test_ok YAML.load("{a: 2007-01-01 01:12:34}")["a"].instance_of?(String)

# JRUBY-1898
val = YAML.load(<<YAML)
---
- foo
- foo
- [foo]
- [foo]
- {foo: foo}
- {foo: foo}
YAML

test_ok val[0].object_id != val[1].object_id
test_ok val[2].object_id != val[3].object_id
test_ok val[4].object_id != val[5].object_id

# JRUBY-1911
val = YAML.load(<<YAML)
---
foo: { bar }
YAML

test_equal({"foo" => {"bar" => nil}}, val)

# JRUBY-1756
# This is almost certainly invalid YAML. but MRI handles it...
val = YAML.load(<<YAML)
---
default: –
- a
YAML

test_equal({"default" => ['a']}, val)

# JRUBY-1978, scalars can start with , if it's not ambigous
test_equal(",a", YAML.load("--- \n,a"))

# Make sure that overriding to_yaml always throws an exception unless it returns the correct thing

class TestYamlFoo
  def to_yaml(*args)
    "foo"
  end
end

test_exception(TypeError) do 
  { :foo => TestYamlFoo.new }.to_yaml
end

# JRUBY-2019, handle tagged_classes, yaml_as and so on a bit better

test_equal({
             "tag:yaml.org,2002:omap"=>YAML::Omap, 
             "tag:yaml.org,2002:pairs"=>YAML::Pairs, 
             "tag:yaml.org,2002:set"=>YAML::Set, 
             "tag:yaml.org,2002:timestamp#ymd"=>Date, 
             "tag:yaml.org,2002:bool#yes"=>TrueClass, 
             "tag:yaml.org,2002:int"=>Integer, 
             "tag:yaml.org,2002:timestamp"=>Time, 
             "tag:yaml.org,2002:binary"=>String, 
             "tag:yaml.org,2002:str"=>String, 
             "tag:yaml.org,2002:map"=>Hash, 
             "tag:yaml.org,2002:null"=>NilClass, 
             "tag:yaml.org,2002:bool#no"=>FalseClass, 
             "tag:yaml.org,2002:seq"=>Array, 
             "tag:yaml.org,2002:float"=>Float,
             "tag:ruby.yaml.org,2002:sym"=>Symbol, 
             "tag:ruby.yaml.org,2002:object"=>Object, 
             "tag:ruby.yaml.org,2002:hash"=>Hash, 
             "tag:ruby.yaml.org,2002:time"=>Time, 
             "tag:ruby.yaml.org,2002:symbol"=>Symbol, 
             "tag:ruby.yaml.org,2002:string"=>String, 
             "tag:ruby.yaml.org,2002:regexp"=>Regexp, 
             "tag:ruby.yaml.org,2002:range"=>Range, 
             "tag:ruby.yaml.org,2002:array"=>Array, 
             "tag:ruby.yaml.org,2002:exception"=>Exception, 
             "tag:ruby.yaml.org,2002:struct"=>Struct, 
           },
           YAML::tagged_classes)


# JRUBY-2083

test_equal({'foobar' => '>= 123'}, YAML.load("foobar: >= 123"))

# JRUBY-2135
test_equal({'foo' => 'bar'}, YAML.load("---\nfoo: \tbar"))

# JRUBY-1911
test_equal({'foo' => {'bar' => nil, 'qux' => nil}}, YAML.load("---\nfoo: {bar, qux}"))

# JRUBY-2323
class YAMLTestException < Exception;end
class YAMLTestString < String; end
test_equal('--- !str:YAMLTestString', YAMLTestString.new.to_yaml.strip)
test_equal(YAMLTestString.new, YAML::load('--- !str:YAMLTestString'))

test_equal(<<EXCEPTION_OUT, YAMLTestException.new.to_yaml) 
--- !ruby/exception:YAMLTestException 
message: YAMLTestException
EXCEPTION_OUT

test_equal(YAMLTestException.new.inspect, YAML::load(YAMLTestException.new.to_yaml).inspect)

# JRUBY-2409
test_equal("*.rb", YAML::load("---\n*.rb"))
test_equal("&.rb", YAML::load("---\n&.rb"))

# JRUBY-2443
a_str = "foo"
a_str.instance_variable_set :@bar, "baz"

test_ok(["--- !str \nstr: foo\n\"@bar\": baz\n", "--- !str \n\"@bar\": baz\nstr: foo\n"].include?(a_str.to_yaml))
test_equal "baz", YAML.load(a_str.to_yaml).instance_variable_get(:@bar)

test_equal :"abc\"flo", YAML.load("---\n:\"abc\\\"flo\"")

# JRUBY-2579
test_equal [:year], YAML.load("---\n[:year]")

test_equal({
             'date_select' => { 'order' => [:year, :month, :day] }, 
             'some' => { 
               'id' => 1, 
               'name' => 'some', 
               'age' => 16}}, YAML.load(<<YAML))
date_select: 
  order: [:year, :month, :day]
some:
    id: 1
    name: some
    age: 16 
YAML


# JRUBY-2754
obj = Object.new
objects1 = [obj, obj]
test_ok objects1[0].object_id == objects1[1].object_id

objects2 = YAML::load objects1.to_yaml
test_ok objects2[0].object_id == objects2[1].object_id

# JRUBY-2192

class FooYSmith < Array; end

obj = YAML.load(<<YAMLSTR)
--- !ruby/array:FooYSmith
- val
- val2
YAMLSTR

test_equal FooYSmith, obj.class


class FooXSmith < Hash; end

obj = YAML.load(<<YAMLSTR)
--- !ruby/hash:FooXSmith
key: value
otherkey: othervalue
YAMLSTR

test_equal FooXSmith, obj.class

# JRUBY-2976
class PersonTestOne
  yaml_as 'tag:data.allman.ms,2008:Person'
end

test_equal "--- !data.allman.ms,2008/Person {}\n\n", PersonTestOne.new.to_yaml
test_equal PersonTestOne, YAML.load(PersonTestOne.new.to_yaml).class



Hash.class_eval do
  def to_yaml( opts = {} )
    YAML::quick_emit( self, opts ) do |out|
      out.map( taguri, to_yaml_style ) do |map|
        each do |k, v|
          map.add( k, v )
        end
      end
    end
  end

end

roundtrip({ "element" => "value", "array" => [ { "nested_element" => "nested_value" } ] })

jruby3639 = <<Y
--- !ruby/object:MySoap::InterfaceOne::DiscountServiceRequestType 
orderRequest: !ruby/object:MySoap::InterfaceOne::OrderType 
  brand: !str 
    str: ""
Y

test_no_exception { YAML.load(jruby3639) }

# JRUBY-3773
class Badger
  attr_accessor :name, :age

  def initialize(name, age)
    @name = name
    @age = age
  end

  def to_s
    "#{name}:#{age}"
  end

  def self.from_s (s)
    ss = s.split(":")
    Badger.new ss[0], ss[1]
  end
end

#
# opening Badger to add custom YAML serialization
#
class Badger
  yaml_as "tag:ruby.yaml.org,2002:#{self}"

  def to_yaml (opts={})
    YAML::quick_emit(self.object_id, opts) do |out|
      out.map(taguri) do |map|
        map.add("s", to_s)
      end
    end
  end

  def Badger.yaml_new (klass, tag, val)
    s = val["s"]
    begin
      Badger.from_s s
    rescue => e
      raise "failed to decode Badger from '#{s}'"
    end
  end
end

b = Badger.new("Axel", 35)

test_equal YAML::dump(b), <<OUT
--- !ruby/Badger 
s: Axel:35
OUT


# JRUBY-3751

class ControlStruct < Struct.new(:arg1)
end

class BadStruct < Struct.new(:arg1)
  def initialize(a1)
    self.arg1 = a1
  end
end

class ControlObject
  attr_accessor :arg1
    
  def initialize(a1)
    self.arg1 = a1
  end
  
  def ==(o)
    self.arg1 == o.arg1
  end
end

class_obj1  = ControlObject.new('class_value')
struct_obj1 = ControlStruct.new
struct_obj1.arg1 = 'control_value'
struct_obj2 = BadStruct.new('struct_value')

test_equal YAML.load(class_obj1.to_yaml), class_obj1
test_equal YAML.load(struct_obj1.to_yaml), struct_obj1
test_equal YAML.load(struct_obj2.to_yaml), struct_obj2


# JRUBY-3518
class Sample
  attr_reader :key
  def yaml_initialize( tag, val )
    @key = 'yaml initialize'
  end
end

class SampleHash < Hash
  attr_reader :key
  def yaml_initialize( tag, val )
    @key = 'yaml initialize'
  end
end

class SampleArray < Array
  attr_reader :key
  def yaml_initialize( tag, val )
    @key = 'yaml initialize'
  end
end

s = YAML.load(YAML.dump(Sample.new))
test_equal 'yaml initialize', s.key 

s = YAML.load(YAML.dump(SampleHash.new))
test_equal 'yaml initialize', s.key 

s = YAML.load(YAML.dump(SampleArray.new))
test_equal 'yaml initialize', s.key


# JRUBY-3327

test_equal YAML.load("- foo\n  bar: bazz"), [{"foo bar" => "bazz"}]

# JRUBY-3263
y = <<YAML
production:
 ABQIAAAAinq15RDnRyoOaQwM_PoC4RTJQa0g3IQ9GZqIMmInSLzwtGDKaBTPoBdSu0WQaPTIv1sXhVRK0Kolfg
 example.com: ABQIAAAAzMUFFnT9uH0Sfg98Y4kbhGFJQa0g3IQ9GZqIMmInSLrthJKGDmlRT98f4j135zat56yjRKQlWnkmod3TB
YAML

test_equal YAML.load(y)['production'], {"ABQIAAAAinq15RDnRyoOaQwM_PoC4RTJQa0g3IQ9GZqIMmInSLzwtGDKaBTPoBdSu0WQaPTIv1sXhVRK0Kolfg example.com" => "ABQIAAAAzMUFFnT9uH0Sfg98Y4kbhGFJQa0g3IQ9GZqIMmInSLrthJKGDmlRT98f4j135zat56yjRKQlWnkmod3TB"}


# JRUBY-3412
y = "--- 2009-02-16 22::40:26.574754 -05:00\n"
test_equal YAML.load(y).to_yaml, y
