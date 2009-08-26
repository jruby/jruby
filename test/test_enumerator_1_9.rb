require 'test/unit'
require 'enumerator'

class TestEnumerator19 < Test::Unit::TestCase
  def test_defined
    assert defined?(Enumerator)

    assert(Enumerator.instance_methods.include?(:next), "Enumerator#next not found")
    assert(Enumerator.instance_methods.include?(:rewind), "Enumerator#rewind not found")
    assert(Enumerator.instance_methods.include?(:each_with_index), "Enumerator#each_with_index not found")
    assert(Enumerator.instance_methods.include?(:with_index), "Enumerator#with_index not found")
    assert(Enumerator.instance_methods.include?(:each_with_object), "Enumerator#each_with_object not found")
    assert(Enumerator.instance_methods.include?(:with_object), "Enumerator#with_object not found")
  end

  def test_each_with_object_no_block_guiven
    assert [].each.each_with_object('').is_a?(Enumerator), 'each_with_object should return an Enumerator'
  end

  def test_with_object_no_block_guiven
    assert [].each.with_object('').is_a?(Enumerator), 'with_object should return an Enumerator'
  end

  def test_each_with_object_block_guiven
    assert_equal 'foobazbarbaz', with_object_expected
  end
  
  def test_with_object_block_guiven
    assert_equal 'foobazbarbaz', with_object_expected
  end

  def test_each_with_index_no_block_guiven
    assert [].each.each_with_index.is_a?(Enumerator), 'each_with_index should return an Enumerator'
  end

  def test_with_index_no_block_guiven
    assert [].each.with_index.is_a?(Enumerator), 'with_index should return an Enumerator'
  end

  def test_each_with_index_block_guiven
    assert_equal 4, with_index_expected
  end

  def test_with_index_block_guiven
    assert_equal 4, with_index_expected
  end

  def with_object_expected
    expected = ''
    %w|foo bar|.each.with_object('baz') {|e, o| expected += e + o}
    expected
  end

  def with_index_expected
    expected = 0
    (1..2).each.with_index {|e, i| expected += e + i}
    expected
  end
end