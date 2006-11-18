require 'test/minirunit'
require 'enumerator'

######################
# Everything in the right place
######################
test_ok(Object.instance_methods.include?("enum_for"))
test_ok(Object.instance_methods.include?("to_enum"))
test_ok(Enumerable.instance_methods.include?("each_slice"))
test_ok(Enumerable.instance_methods.include?("each_cons"))
test_ok(Enumerable.instance_methods.include?("enum_with_index"))
test_ok(Enumerable.instance_methods.include?("enum_slice"))
test_ok(Enumerable.instance_methods.include?("enum_cons"))
test_ok(Enumerable::Enumerator.public_methods.include?("new"))
test_ok(!Enumerable::Enumerator.instance_methods.include?("initialize"))
test_ok(Enumerable::Enumerator.instance_methods.include?("each"))
test_ok(Enumerable::Enumerator.ancestors.include?(Enumerable))

######################
# Object#enum_for & Object#to_enum
######################
test_ok([97, 98, 99], "abc".enum_for(:each_byte).map { |b| b })
test_ok([97, 98, 99], "abc".to_enum(:each_byte).map { |b| b })

class Wobble
  def each
    3.times do |index|
      yield(1) if block_given?
    end
  end
  def each_with_args(count, multiplier)
    count.times do |index|
      yield(index * multiplier) if block_given?
    end
  end
  def each_needing_block
    yield(123)
  end
  def each_not_needing_block
  end
end

#defaults to enumerating over :each
test_ok([1, 1, 1], Wobble.new.enum_for.map { |b| b })
test_ok([1, 1, 1], Wobble.new.to_enum.map { |b| b })

#parameters passed to enumerator method
test_ok([0, 10, 20], Wobble.new.enum_for(:each_with_args, 3, 10).map { |b| b })
test_ok([0, 10, 20], Wobble.new.to_enum(:each_with_args, 3, 10).map { |b| b })

######################
# Enumerable#enum_with_index
######################

test_equal(
  [['a', 0], ['b', 1], ['c', 2]],
  ['a', 'b', 'c'].enum_with_index.map { |item| item })

test_equal([], [].enum_with_index.map { |item| item })

######################
# Enumerable#enum_slice and Enumerable#each_slice
######################

#It does what it says on the tin!
test_equal([[1], [2], [3]], Enumerable::Enumerator.new([1,2,3], :each_slice, 1).map { |item| item })
test_equal([[1], [2], [3]], [1,2,3].enum_slice(1).map { |item| item })

test_equal([[1,2], [3,4]], Enumerable::Enumerator.new([1,2,3,4], :each_slice, 2).map { |item| item })
test_equal([[1,2], [3,4]], [1,2,3,4].enum_slice(2).map { |item| item })

test_equal([[1,2], [3,4], [5]], Enumerable::Enumerator.new([1,2,3,4,5], :each_slice, 2).map { |item| item })
test_equal([[1,2], [3,4], [5]], [1,2,3,4,5].enum_slice(2).map { |item| item })

#Jagged array not flattened
test_equal([[[1,2],3],[4,5]], Enumerable::Enumerator.new([[1,2],3,4,5], :each_slice, 2).map { |item| item })
test_equal([[[1,2],3],[4,5]], [[1,2],3,4,5].enum_slice(2).map { |item| item })
#empty array
test_equal([], Enumerable::Enumerator.new([], :each_slice, 3).map { |item| item })
test_equal([], [].enum_slice(3).map { |item| item })
#slice count == array count yields
test_equal([[1,2,3]], Enumerable::Enumerator.new([1,2,3], :each_slice, 3).map { |item| item })
test_equal([[1,2,3]], [1,2,3].enum_slice(3).map { |item| item })
#slice count > array count yields array
test_equal([[1,2,3]], Enumerable::Enumerator.new([1,2,3], :each_slice, 4).map { |item| item })
test_equal([[1,2,3]], [1,2,3].enum_slice(4).map { |item| item })

#slice size must be specified
test_exception(ArgumentError) { [].each_cons {} }
test_exception(ArgumentError) { [].enum_cons }

#slice size must be > 0 but error only raised on each
test_exception(ArgumentError) { [1,2,3].each_slice(0) {} }
test_no_exception { [1,2,3].enum_slice(0) }
test_exception(ArgumentError) { [1,2,3].enum_slice(0).each {} }
test_exception(ArgumentError) { [1,2,3].each_slice(-1) {} }
test_no_exception { [1,2,3].enum_slice(-1) }
test_exception(ArgumentError) { [1,2,3].enum_slice(-1).each {} }

#slice size must convertible to integer but error only raised on each
test_exception(TypeError) { [].each_slice(Object.new) {} }
test_no_exception { [].enum_slice(Object.new) }
test_exception(TypeError) { [].enum_slice(Object.new).each {} }

#enum_slice & each_slice always return nil
test_equal(nil, [0,1,2].each_slice(2) { 123 })
test_equal(nil, [0,1,2].enum_slice(2).each { 123 })

######################
# Enumerable#enum_cons and Enumerable#each_const
######################

#It does what it says on the tin!
test_equal([[1,2], [2,3], [3,4], [4,5]], Enumerable::Enumerator.new([1,2,3,4,5], :each_cons, 2).map { |item| item })
test_equal([[1,2], [2,3], [3,4], [4,5]], [1,2,3,4,5].enum_cons(2).map { |item| item })

#Jagged array not flattened
test_equal([[[1,2],3], [3,4], [4,5]], Enumerable::Enumerator.new([[1,2],3,4,5], :each_cons, 2).map { |item| item })
test_equal([[[1,2],3], [3,4], [4,5]], [[1,2],3,4,5].enum_cons(2).map { |item| item })
#cons count == array size yields once
test_equal([[1,2,3]], Enumerable::Enumerator.new([1,2,3], :each_cons, 3).map { |item| item })
test_equal([[1,2,3]], [1,2,3].enum_cons(3).map { |item| item })
#cons count > array size yields nothing
test_equal([], Enumerable::Enumerator.new([1,2,3], :each_cons, 4).map { |item| item.to_s })
test_equal([], [1,2,3].enum_cons(4).map { |item| item.to_s })

#cons size must be specified
test_exception(ArgumentError) { [].each_cons {} }
test_exception(ArgumentError) { [].enum_cons }

#cons size must be > 0 but error only raised on each
test_exception(ArgumentError) { [].each_cons(0) {} }
test_no_exception { [].enum_cons(0) }
test_exception(ArgumentError) { [].enum_slice(0).each {} }
test_exception(ArgumentError) { [].each_slice(-1) {} }
test_no_exception { [].enum_cons(-1) }
test_exception(ArgumentError) { [].enum_slice(-1).each {} }

#cons size must convertible to integer but error only raised on each
test_exception(TypeError) { [].each_cons(Object.new) {} }
test_no_exception { [].enum_cons(Object.new) }
test_exception(TypeError) { [].enum_cons(Object.new).each {} }

#enum_cons & each_cons always returns nil
test_equal(nil, [0,1,2].enum_cons(2).each { })
test_equal(nil, [0,1,2].each_cons(2) { })

######################
# Enumerable::Enumerator
######################

test_exception(ArgumentError) { Enumerable::Enumerator.new }
test_no_exception { Enumerable::Enumerator.new("somestring") }

#NoMethodError if enumeration method doesn't exist
test_exception(NoMethodError) { Enumerable::Enumerator.new(Object.new).each {} }
test_exception(NoMethodError) { Enumerable::Enumerator.new("abc", :each_BYTE).each {} }

#each works!
test_equal([97, 98, 99], Enumerable::Enumerator.new("abc", :each_byte).map { |b| b })

#no block results in LocalJumpError only if enumerating method requires it
test_exception(LocalJumpError) { Enumerable::Enumerator.new(Wobble.new, :each_needing_block).each }
test_no_exception { Enumerable::Enumerator.new(Wobble.new, :each_not_needing_block).each }