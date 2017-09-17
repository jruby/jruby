require 'test/unit'
require 'set.rb'

# JRuby's Set impl specific or low-level details.
class TestSet < Test::Unit::TestCase

  class SubSet < Set ; end
  class SubSortedSet < SortedSet ; end

  def test_sub_set
    set = SubSet.new
    assert_same SubSet, set.class
    assert set.is_a?(SubSet)
    assert set.is_a?(Set)
    assert hash = set.instance_variable_get(:@hash)
    assert hash.is_a?(Hash)
    assert_equal Hash.new, hash
    assert_equal '#<TestSet::SubSet: {}>', set.inspect

    assert_false Set.new.equal?(SubSet.new)
    assert_true Set.new.eql?(SubSet.new)
    assert_true ( SubSet.new == Set.new )

    assert_false SortedSet.new.equal?(SubSortedSet.new)
    assert_true SortedSet.new.eql?(SubSortedSet.new)
    assert_true ( SubSortedSet.new == Set.new )
    assert_true ( SubSortedSet.new == SortedSet.new )
    assert_true ( SortedSet.new == SubSortedSet.new )
  end

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
    assert_equal "\x04\b{\x00".force_encoding('ASCII-8BIT'), Marshal.dump(Hash.new)

    # MRI internally uses a @hash with a default: `Hash.new(false)'
    empty_set = "\x04\bo:\bSet\x06:\n@hash}\x00F".force_encoding('ASCII-8BIT')
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

  def test_sorted_marshal_dump
    dump = Marshal.dump(SortedSet.new)
    assert_equal SortedSet.new, Marshal.load(dump)

    set = Marshal.load Marshal.dump(SortedSet.new([2, 1]))
    assert_same SortedSet, set.class
    assert_equal SortedSet.new([1, 2]), set
    assert_equal [1, 2], set.sort
    set << 3
    assert_equal 3, set.size
    assert_equal [1, 2, 3], set.sort

    set = Marshal.load Marshal.dump(SortedSet.new([2, 1]).dup)
    assert_same SortedSet, set.class
    assert_equal SortedSet.new([1, 2]), set
    assert_equal [1, 2], set.to_a

    set = Marshal.load Marshal.dump(SortedSet.new([2, 3, 1]))
    each = []; set.each { |e| each << e }
    assert_equal [1, 2, 3], each
  end

  def test_dup
    set = Set.new [1, 2]
    assert_same Set, set.dup.class
    assert_equal set, set.dup
    dup = set.dup
    set << 3
    assert_equal 3, set.size
    assert_equal 2, dup.size

    set = SortedSet.new [1, 2]
    assert_same SortedSet, set.dup.class
    assert_equal set, set.dup
    dup = set.dup
    set << 0
    assert_equal 3, set.size
    assert_equal 2, dup.size
  end

  def test_to_java
    assert set = Set.new.to_java
    assert set.toString.start_with?('#<Set:0x')
    assert_equal org.jruby.ext.set.RubySet, set.class
    assert set.is_a?(java.util.Set)
    assert_equal java.util.HashSet.new, set

    assert set = SortedSet.new([2, 1]).to_java
    assert set.toString.start_with?('#<SortedSet:0x')
    assert_equal org.jruby.ext.set.RubySortedSet, set.class
    assert set.is_a?(java.util.Set)
    assert set.is_a?(java.util.SortedSet)
    assert_equal java.util.TreeSet.new([1, 2]), set
  end if defined? JRUBY_VERSION

end
