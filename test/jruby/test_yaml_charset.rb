require 'test/unit'

class TestYamlCharset < Test::Unit::TestCase

  def setup
    require 'yaml'
  end

  JAPANESE = "公園では楽しいイベントがいっぱい！今月のイベント一覧をチェックして、ぜひみなさんでお出かけください。"
   
  def test_dumb
    original = JAPANESE.to_s
    assert_equal original.to_s, JAPANESE
  end

  # JRUBY-1219
  def test_yaml_with_japanese
    do_test JAPANESE
  end
  
  def do_test original
    dump = YAML.dump original
    loaded = YAML.load dump
    assert_equal original, loaded
  end

  def test_vietnamese_eej
    nguyen = "nguy\341\273\205n"
    assert_equal nguyen, YAML::load(nguyen.to_yaml)
  end

end
