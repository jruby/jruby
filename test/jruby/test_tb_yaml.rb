# JRUBY-1219
require 'test/unit'
require "yaml"
require "pp"

class TestTbYaml < Test::Unit::TestCase
  JAPANESE = "公園では楽しいイベントがいっぱい！今月のイベント一覧をチェックして、ぜひみなさんでお出かけください。"
   
  def test_dumb
    original = JAPANESE.to_s
    assert_equal original.to_s, JAPANESE
  end
            
  def test_yaml_with_japanese
    do_test JAPANESE
  end
  
  def do_test original
    dump = YAML.dump original
    loaded = YAML.load dump
    assert_equal original, loaded
  end
end
