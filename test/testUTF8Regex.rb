require 'test/minirunit'
test_check "Test UTF8 Regexp"

# all offsets should be on character boundaries to work correctly
# oops, it looks like expected should be test_equal's first argument, not the second

# test =~
test_equal(6, "スパムハンク" =~ /ム(.)/u)
test_equal(3, $1.size)
test_equal("ハ", $1)

# test []
test_equal("ムハ", "スパムハンク"[/ム./u])

# test []=
x = "スパムハンク"
x[/ム./u] = "ス"
test_equal("スパスンク", x)

# test index
test_equal(6, "スパムハンク".index(/ム./u))

# test index with offset
test_equal(6, "スパムハンク".index(/ム./u, 3))
test_equal(nil, "スパムハンク".index(/ム./u, 9))

# test rindex
test_equal(6, "スパムハンク".rindex(/ム./u))

# test rindex with offset
test_equal(6, "スパムハンク".rindex(/ム./u))
# This doesn't pass under Ruby 1.8.6 either, it returns "6"
#test_equal(nil, "スパムハンク".rindex(/ム./u, 9))

# test scan
test_equal(6, "スパムハンク".scan(/./u).size)

# test slice!
x = "スパムハンク"
test_equal("ムハ", x.slice!(/ム./u))
test_equal("スパンク", x)

# test split
x = "スパムハンク".split(/ム/u)
test_equal(x.size, 2)
test_equal("スパ", x[0])
test_equal("ハンク", x[1])

