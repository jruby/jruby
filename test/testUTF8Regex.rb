
require 'test/minirunit'
test_check "Test UTF8 Regexp"

# test =~
test_equal(6, "スパムハンク" =~ /ム(.)/u)
test_equal(3, $1.size)
test_equal("ハ", $1)
test_equal(14, "Ruby:公式のルビーinterprété orienté objet" =~ /.ビ(ー[a-z]+é).é/u)
test_equal(12, $1.size)
test_equal("ーinterpré", $1)

# test []
test_equal("ムハ", "スパムハンク"[/ム./u])
test_equal("ーinterpré", "Ruby:公式のルビーinterprété orienté objet"[/ー[a-z]+é/u])

# test []=
x = "スパムハンク"
x[/ム./u] = "ス"
test_equal("スパスンク", x)
x = "公式のルビーinterprété orienté objet"
x[/の.*je/u] = "スンa"
test_equal("公式スンat", x)

# test index
test_equal(6, "スパムハンク".index(/ム./u))
test_equal(20, "Ruby:公式のルビーinterprété orienté objet".index(/ー[a-z]+é/u))

# test index with offset
test_equal(6, "スパムハンク".index(/ム./u, 3))
test_equal(nil, "スパムハンク".index(/ム./u, 9))
test_equal(20, "Ruby:公式のルビーinterprété orienté objet".index(/ー[a-z]+é/u, 4))
test_equal(nil, "Ruby:公式のルビーinterprété orienté objet".index(/ー[a-z]+é/u, 23))

# test rindex
test_equal(6, "スパムハンク".rindex(/ム./u))
test_equal(20, "Ruby:公式のルビーinterprété orienté objet".rindex(/ー[a-z]+é/u))

# test rindex with offset
test_equal(nil, "スパムハンク".rindex(/ム./u, 3))
test_equal(6, "スパムハンク".rindex(/./u, 6))
test_equal(nil, "Ruby:公式のルビーinterprété orienté objet".rindex(/[a-z]+é/u, 20))
test_equal(32, "Ruby:公式のルビーinterprété orienté objet".rindex(/[a-z]+é/u, 35))

# test scan
test_equal(6, "スパムハンク".scan(/./u).size)
test_equal(35, "Ruby:公式のルビーinterprété orienté objet".scan(/./u).size)

# test slice!
x = "スパムハンク"
test_equal("ムハ", x.slice!(/ム./u))
test_equal("スパンク", x)
x = "Ruby:公式のルビーinterprété orienté objet"
test_equal("ーinterpré", x.slice!(/ー[a-z]+é/u))
test_equal("Ruby:公式のルビté orienté objet", x)

# test split
x = "スパムハンク".split(/ム/u)
test_equal(2, x.size)
test_equal("スパ", x[0])
test_equal("ハンク", x[1])
x = "Ruby:公式のルビーinterprété orienté objet".split(/e/u)
test_equal(4, x.size)
test_equal("Ruby:公式のルビーint", x[0])
test_equal("rprété ori", x[1])
test_equal("nté obj", x[2])
test_equal("t", x[3])

