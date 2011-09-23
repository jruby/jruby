require 'test/unit'

class MagicCommentTest19 < Test::Unit::TestCase

  # JRUBY-5922
  def test_magic_comment_parse
    path = File.join(File.dirname(__FILE__), 'runaway_magic_comment.rb')
    assert(system "#{ENV_JAVA['jruby.home']}/bin/jruby --1.9 #{path}")
  end
end
