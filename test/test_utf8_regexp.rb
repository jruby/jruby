require 'test/unit'

class TestUTF8Regexp < Test::Unit::TestCase
  def test_match_operator
    assert_equal(6, "スパムハンク" =~ /ム(.)/u)
    assert_equal(3, $1.size)
    assert_equal("ハ", $1)
    assert_equal(14, "Ruby:公式のルビーinterprété orienté objet" =~ /.ビ(ー[a-z]+é).é/u)
    assert_equal(12, $1.size)
    assert_equal("ーinterpré", $1)
  end

  def test_index_operator
    assert_equal("ムハ", "スパムハンク"[/ム./u])
    assert_equal("ーinterpré", "Ruby:公式のルビーinterprété orienté objet"[/ー[a-z]+é/u])
  end

  def test_index_assignment_operator
    x = "スパムハンク"
    x[/ム./u] = "ス"
    assert_equal("スパスンク", x)
    x = "公式のルビーinterprété orienté objet"
    x[/の.*je/u] = "スンa"
    assert_equal("公式スンat", x)
  end

  def test_index
    assert_equal(6, "スパムハンク".index(/ム./u))
    assert_equal(20, "Ruby:公式のルビーinterprété orienté objet".index(/ー[a-z]+é/u))
  end

  def  test_index_with_offset
    assert_equal(6, "スパムハンク".index(/ム./u, 3))
    assert_equal(nil, "スパムハンク".index(/ム./u, 9))
    assert_equal(20, "Ruby:公式のルビーinterprété orienté objet".index(/ー[a-z]+é/u, 4))
    assert_equal(nil, "Ruby:公式のルビーinterprété orienté objet".index(/ー[a-z]+é/u, 23))
  end

  def test_rindex
    assert_equal(6, "スパムハンク".rindex(/ム./u))
    assert_equal(20, "Ruby:公式のルビーinterprété orienté objet".rindex(/ー[a-z]+é/u))
  end

  def test_rindex_with_offset
    assert_equal(nil, "スパムハンク".rindex(/ム./u, 3))
    assert_equal(6, "スパムハンク".rindex(/./u, 6))
    assert_equal(nil, "Ruby:公式のルビーinterprété orienté objet".rindex(/[a-z]+é/u, 20))
    assert_equal(32, "Ruby:公式のルビーinterprété orienté objet".rindex(/[a-z]+é/u, 35))
  end

  def test_scan
    assert_equal(6, "スパムハンク".scan(/./u).size)
    assert_equal(35, "Ruby:公式のルビーinterprété orienté objet".scan(/./u).size)
  end

  def test_slice!
    x = "スパムハンク"
    assert_equal("ムハ", x.slice!(/ム./u))
    assert_equal("スパンク", x)
    x = "Ruby:公式のルビーinterprété orienté objet"
    assert_equal("ーinterpré", x.slice!(/ー[a-z]+é/u))
    assert_equal("Ruby:公式のルビté orienté objet", x)
  end

  def test_split
    x = "スパムハンク".split(/ム/u)
    assert_equal(2, x.size)
    assert_equal("スパ", x[0])
    assert_equal("ハンク", x[1])
    x = "Ruby:公式のルビーinterprété orienté objet".split(/e/u)
    assert_equal(4, x.size)
    assert_equal("Ruby:公式のルビーint", x[0])
    assert_equal("rprété ori", x[1])
    assert_equal("nté obj", x[2])
    assert_equal("t", x[3])
  end

end