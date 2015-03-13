# Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# Truffle doesn't have a compliant random number generator yet

SMALL_RANDOM = [
  5, 7, 1, 8, 3,
  3, 2, 9, 1, 5,
  2, 6, 4, 3, 3,
  4, 7, 3, 6, 9,
  6, 6, 3, 1, 2,
  4, 1, 8, 5, 7,
  2, 7, 0, 6, 9,
  6, 0, 3, 0, 5,
  5, 4, 8, 9, 7,
  1, 6, 8, 7, 5,
  9, 4, 8, 3, 6,
  6, 3, 4, 5, 6,
  3, 5, 8, 0, 3,
  3, 3, 0, 0, 0,
  8, 8, 4, 0, 3,
  6, 4, 3, 6, 6,
  3, 4, 7, 0, 8,
  2, 3, 4, 3, 9,
  5, 7, 0, 1, 4,
  2, 4, 2, 8, 6
]

BIG_RANDOM = [
  18, 82, 58, 32, 94,
  69, 98, 38, 30, 0,
  10, 61, 70, 97, 83,
  44, 72, 15, 3, 44,
  64, 53, 46, 2, 58,
  9, 13, 39, 17, 40,
  35, 28, 20, 81, 7,
  77, 43, 48, 56, 71,
  43, 66, 35, 77, 29,
  35, 73, 9, 89, 26,
  54, 93, 1, 14, 9,
  30, 77, 81, 75, 62,
  79, 55, 58, 77, 8,
  74, 4, 18, 31, 1,
  82, 90, 34, 53, 21,
  99, 82, 54, 26, 43,
  35, 65, 35, 45, 84,
  66, 27, 7, 1, 79,
  20, 54, 78, 20, 73,
  49, 5, 31, 61, 0
]

@small_random = 0

def small_random
  SMALL_RANDOM[(@small_random += 1) % SMALL_RANDOM.size]
end

@big_random = 0

def big_random
  BIG_RANDOM[(@big_random += 1) % BIG_RANDOM.size]
end

def random_range(range)
  big_random % range
end

$assert_index = 0

def assert(condition)
  raise "failure on #{$assert_index}" unless condition
  $assert_index += 1
end

def random_hash
  case random_range(5)
  when 0
    {}
  when 1
    Hash.new
  when 2
    eval("{" + small_random.times.map { |n| "#{small_random} => #{small_random}" }.join(", ") + "}")
  when 3
    eval("{" + small_random.times.map { |n| "#{big_random} => #{big_random}" }.join(", ") + "}")
  when 4
    eval("{" + big_random.times.map { |n| "#{big_random} => #{big_random}" }.join(", ") + "}")
  end
end

def check_hash(hash)
  assert eval(hash.inspect) == hash
  assert hash == eval(hash.inspect)
  assert hash.keys.size == hash.size
  assert hash.keys.uniq == hash.keys
end

1_000.times do
  # Create a hash

  hash = random_hash

  check_hash(hash)

  100.times do
    # Perform a random mutation

    case random_range(7)
    when 0
      # Clear
      hash.clear
      assert hash.size == 0
    when 1
      # Merge with a new hash creating a new hash
      original_size = hash.size
      hash = hash.merge(random_hash)
      assert hash.size >= original_size
    when 2
      # Set a big random key
      original_size = hash.size
      key = big_random
      value = big_random
      hash[key] = value
      assert hash[key] == value
      assert (hash.size == original_size) || (hash.size == original_size + 1)
    when 3
      # Set a small random key
      original_size = hash.size
      key = small_random
      value = small_random
      hash[key] = value
      assert hash[key] == value
      assert (hash.size == original_size) || (hash.size == original_size + 1)
    when 4
      # Delete a big random key - if it exists
      original_size = hash.size
      hash.delete(big_random)
      assert (hash.size == original_size) || (hash.size == original_size - 1)
    when 5
      # Delete a small random key - if it exists
      original_size = hash.size
      hash.delete(small_random)
      assert (hash.size == original_size) || (hash.size == original_size - 1)
    when 6
      # Delete a random one of the actual keys
      if hash.size > 0
        original_size = hash.size
        hash.delete(hash.keys[big_random % hash.keys.size])
        assert (hash.size == original_size - 1)
      end
    end

    check_hash(hash)
  end
end

1_000.times do
  # Create a hash

  hash = {}

  # Add random elements, remembering the order we put them in

  keys = []

  100.times do
    key = big_random
    keys << key
    hash[key] = big_random
  end

  # Check the order we get out is the same

  assert hash.keys == keys.uniq
end
