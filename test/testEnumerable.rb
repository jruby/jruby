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

