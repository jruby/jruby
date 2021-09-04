class GH4218Enumerable
  include Enumerable

  def initialize(elements)
    @elements = elements
  end

  def each(&block)
    @elements.each(&block)
  end
end

describe '#4218 Enumerable#drop' do
  it 'does not change the identity of the elements' do
    original = [Object.new, Object.new, Object.new]
    enumerable = GH4218Enumerable.new(original)
    expect(original[1]).to be_equal(enumerable.drop(1).first)
    expect(enumerable.to_a[1]).to be_equal(enumerable.drop(1).first)
  end
end
