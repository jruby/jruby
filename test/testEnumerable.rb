require 'test/minirunit'

E_STRS = ["333", "22", "666666", "1", "55555", "1010101010"]


#----- min/max -----
test_equal("a",      ["b", "a", "c"].min)
test_equal("11",     ["2","33","4","11"].min {|a,b| a <=> b })
test_equal(2,        [2 , 33 , 4 , 11  ].min {|a,b| a <=> b })
test_equal("4",      ["2","33","4","11"].min {|a,b| b <=> a })
test_equal(33,       [ 2 , 33 , 4 , 11 ].min {|a,b| b <=> a })
test_equal("1",      E_STRS.min {|a,b| a.length <=> b.length })
test_equal("1",      E_STRS.min {|a,b| a <=> b })
test_equal("1",      E_STRS.min {|a,b| a.to_i <=> b.to_i })
# rubicon has the following 2 tests with an expectation of Fixnum 1
# probably a consequence of its equality test methods
test_equal("1",      E_STRS.min {|a,b| a <=> b })
test_equal("1",      E_STRS.min {|a,b| a.to_s <=> b.to_s })

require 'test/minirunit'

test1 = [1,3,5,7,0, 2,43,53,6352,44,221,5]
test2 = (1...14)
test3 = %w(rhea kea flea)
test4 = (1..10)
test5 = %w(apple pear fig)
test6 = 1..100

test_equal(test1, test1.to_a)
test_equal([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13], test2.to_a)
test_equal(test1, test1.entries)
test_equal([0, 1, 2, 3, 5, 5, 7, 43, 44, 53, 221, 6352],test1.sort)
test_equal(["flea", "kea", "rhea"],test3.sort)
test_equal([10, 9, 8, 7, 6, 5, 4, 3, 2, 1],test4.sort { |a,b| b<=>a })
test_equal(["fig", "pear", "apple"],test5.sort_by { |word| word.length })
test_equal([38, 39, 40, 41, 42, 43, 44], test6.grep(38..44))
test_equal(nil,(1..10).detect {|i| i % 5 == 0 and i % 7 == 0 })
test_equal(35,(1..100).detect {|i| i % 5 == 0 and i % 7 == 0 })
test_equal(37,(1..10).detect(lambda { 37 }) {|i| i % 5 == 0 and i % 7 == 0 })
test_equal([3, 6, 9],(1..10).select {|i|  i % 3 == 0 })
test_equal([1, 2, 4, 5, 7, 8, 10],(1..10).reject {|i|  i % 3 == 0 })
test_equal([1, 4, 9, 16],(1..4).collect {|i| i*i })
test_equal(["cat", "cat", "cat", "cat"],(1..4).collect { "cat"  })
test_equal(45,(5..10).inject {|sum, n| sum + n })
test_equal(151200,(5..10).inject(1) {|product, n| product * n })
test_equal("sheep",%w{ cat sheep bear }.inject { |memo,word|  memo.length > word.length ? memo : word})
test_equal(5,%w{ cat sheep bear }.inject(0) { |memo,word| memo >= word.length ? memo : word.length })
test_equal([[2, 4, 6], [1, 3, 5]],(1..6).partition {|i| (i&1).zero?})
  hash1 = Hash.new
  %w(cat dog wombat).each_with_index {|item, index|
    hash1[item] = index
  }
test_equal({"cat"=>0, "wombat"=>2, "dog"=>1},hash1)
test_equal(true,(1..10).include?(5))
test_equal(false,(1..10).include?(13))
test_equal("horse",%w(albatross dog horse).max)
test_equal("albatross",%w(albatross dog horse).max {|a,b| a.length <=> b.length })
test_equal("albatross",%w(albatross dog horse).min)
test_equal("dog",%w(albatross dog horse).min {|a,b| a.length <=> b.length })
test_equal(true,%w{ ant bear cat}.all? {|word| word.length >= 3})
test_equal(false,%w{ ant bear cat}.all? {|word| word.length >= 4})
test_equal(false,[ nil, true, 99 ].all?)
test_equal(true,%w{ ant bear cat}.any? {|word| word.length >= 3})
test_equal(true,%w{ ant bear cat}.any? {|word| word.length >= 4})
test_equal(true,[ nil, true, 99 ].any?)
  a = [ 4, 5, 6 ]
  b = [ 7, 8, 9 ]
test_equal([[1, 4, 7], [2, 5, 8], [3, 6, 9]],(1..3).zip(a, b))
test_equal([["cat\n", 1], ["dog", nil]],"cat\ndog".zip([1]))
test_equal([[1], [2], [3]],(1..3).zip)

test_exception(ArgumentError) {
  ['a'].grep {/foo/}
}

test_equal([Array],[['foo']].map {|a|a.class})
