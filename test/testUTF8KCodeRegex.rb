require 'test/minirunit'
test_check "Test UTF8 KCode Regexp"

old_code = $KCODE
$KCODE = 'u'

# Regexp#kcode should be nil even if $KCODE is set
test_equal(nil, /test/.kcode)

# test =~
test_equal(6, "スパムハンク" =~ /ム(.)/)
test_equal(3, $1.size)
test_equal("ハ", $1)
test_equal(14, "Ruby:公式のルビーinterprété orienté objet" =~ /.ビ(ー[a-z]+é).é/)
test_equal(12, $1.size)
test_equal("ーinterpré", $1)

# test []
test_equal("ムハ", "スパムハンク"[/ム./])
test_equal("ーinterpré", "Ruby:公式のルビーinterprété orienté objet"[/ー[a-z]+é/])

# test []=
x = "スパムハンク"
x[/ム./] = "ス"
test_equal("スパスンク", x)
x = "公式のルビーinterprété orienté objet"
x[/の.*je/] = "スンa"
test_equal("公式スンat", x)

# test index
test_equal(6, "スパムハンク".index(/ム./))
test_equal(20, "Ruby:公式のルビーinterprété orienté objet".index(/ー[a-z]+é/))

# test index with offset
test_equal(6, "スパムハンク".index(/ム./, 3))
test_equal(nil, "スパムハンク".index(/ム./, 9))
test_equal(20, "Ruby:公式のルビーinterprété orienté objet".index(/ー[a-z]+é/, 4))
test_equal(nil, "Ruby:公式のルビーinterprété orienté objet".index(/ー[a-z]+é/, 23))

# test rindex
test_equal(6, "スパムハンク".rindex(/ム./))
test_equal(20, "Ruby:公式のルビーinterprété orienté objet".rindex(/ー[a-z]+é/))

# test rindex with offset
test_equal(nil, "スパムハンク".rindex(/ム./, 3))
test_equal(6, "スパムハンク".rindex(/./, 6))
test_equal(nil, "Ruby:公式のルビーinterprété orienté objet".rindex(/[a-z]+é/, 20))
test_equal(32, "Ruby:公式のルビーinterprété orienté objet".rindex(/[a-z]+é/, 35))

# test scan
test_equal(6, "スパムハンク".scan(/./).size)
test_equal(35, "Ruby:公式のルビーinterprété orienté objet".scan(/./).size)

# test slice!
x = "スパムハンク"
test_equal("ムハ", x.slice!(/ム./))
test_equal("スパンク", x)
x = "Ruby:公式のルビーinterprété orienté objet"
test_equal("ーinterpré", x.slice!(/ー[a-z]+é/))
test_equal("Ruby:公式のルビté orienté objet", x)

# test split
x = "スパムハンク".split(/ム/)
test_equal(2, x.size)
test_equal("スパ", x[0])
test_equal("ハンク", x[1])
x = "Ruby:公式のルビーinterprété orienté objet".split(/e/)
test_equal(4, x.size)
test_equal("Ruby:公式のルビーint", x[0])
test_equal("rprété ori", x[1])
test_equal("nté obj", x[2])
test_equal("t", x[3])

$KCODE = 'n'

pattern = /ス(.)/
target = "テスト"

test_equal(3, target =~ pattern)
test_equal("\343", $1)

$KCODE = 'u'

test_equal(3, target =~ pattern)
# See JRUBY-1133; we have decided not to fix this
#test_equal("ト", $1)

$KCODE = old_code
