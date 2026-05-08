require 'test/unit'
require 'set.rb'

# JRuby's Set impl specific or low-level details.
class TestSet < Test::Unit::TestCase

  def test_allocate
    set = Set.allocate
    assert_same Set, set.class
    assert_nil set.instance_variable_get(:@hash) # same on MRI

    # set not really usable :
    begin
      set << 1 ; fail 'set << 1 did not fail!'
    rescue # NoMethodError # JRuby: NPE
      # NoMethodError: undefined method `[]=' for nil:NilClass
      #   from /opt/local/rvm/rubies/ruby-2.3.3/lib/ruby/2.3.0/set.rb:313:in `add'
    end
  end

  def test_marshal_dump
    assert_equal (+"\x04\b{\x00").force_encoding('ASCII-8BIT'), Marshal.dump(Hash.new)

    # MRI internally uses a @hash with a default: `Hash.new(false)'
    empty_set = (+"\x04\bo:\bSet\x06:\n@hash}\x00F").force_encoding('ASCII-8BIT')
    assert_equal empty_set, Marshal.dump(Set.new)

    dump = Marshal.dump(Set.new)
    assert_equal Set.new, Marshal.load(dump)

    set = Marshal.load Marshal.dump(Set.new([1, 2]))
    assert_same Set, set.class
    assert_equal Set.new([1, 2]), set
    set << 3
    assert_equal 3, set.size

    set = Marshal.load Marshal.dump(Set.new([1, 2]).dup)
    assert_same Set, set.class
    assert_equal Set.new([1, 2]), set
  end

  def test_yaml_dump; require 'yaml'
    str = YAML.dump(Set.new)
    assert_equal "--- !ruby/object:Set\nhash: {}\n", str
    set = YAML.load(str, permitted_classes: [Set])
    assert_equal Set.new([]), set
  end

  def test_dup
    set = Set.new [1, 2]
    assert_same Set, set.dup.class
    assert_equal set, set.dup
    dup = set.dup
    set << 3
    assert_equal 3, set.size
    assert_equal 2, dup.size

  end

  def test_dup_to_a
    set = Set[1, 2]
    assert_equal set.to_a, set.dup.to_a
    assert_equal set.to_a, set.clone.dup.to_a

  end

  def test_to_java
    assert set = Set.new.to_java
    assert_equal "Set[]", set.toString
    assert_equal org.jruby.ext.set.RubySet, set.class
    assert set.is_a?(java.util.Set)
    assert_equal java.util.HashSet.new, set
  end if defined? JRUBY_VERSION

  def test_cmp_0_but_not_eql
    set = Set[1, 2]
    assert_equal set.to_a, set.dup.to_a
    assert_equal set.to_a, set.clone.dup.to_a
  end

  class CmpObj1
    attr_reader :time

    def initialize(time); @time = time end

    def <=>(other)
      time <=> other.time
    end

    def ==(other)
      object_id == other.object_id
    end
  end


  def test_cmp_0_but_not_eql2
    set = Set[1, 2]
    assert_equal set.to_a, set.dup.to_a
    assert_equal set.to_a, set.clone.dup.to_a
  end

  class CmpObj2
    attr_reader :time

    def initialize(time); @time = time end

    def <=>(other)
      time <=> other.time
    end

    def eql?(other)
      object_id == other.object_id
    end
  end

end
