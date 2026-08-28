describe 'Enumerable#each_with_object' do

  it 'reports argument error when no arguments given' do
    begin
      [].each_with_object
      fail 'ArgumentError not raised!'
    rescue ArgumentError => e
      # TODO JRuby does seem to get a different error message than MRI
      #expect( e.message ).to eql 'wrong number of arguments (0 for 1)'
    end
  end

  it 'works with a 2 arg lambda passed as block' do
    storategy = lambda { |str, hash| hash[str] = str }
    result = %w(a b c d e f).each_with_object({}, &storategy)
    expected = {"a"=>"a", "b"=>"b", "c"=>"c", "d"=>"d", "e"=>"e", "f"=>"f"}
    expect( result ).to eql expected
  end

  it 'works just fine with a block' do
    result = %w(a b c d e f).each_with_object({}) { |v, hash| hash[v] = v }
    expected = {"a"=>"a", "b"=>"b", "c"=>"c", "d"=>"d", "e"=>"e", "f"=>"f"}
    expect( result ).to eql expected

    other = []
    result = %w(a b c d e f).each_with_object({}) { |v| other << v }
    expect( result ).to be_empty
    expect( other ).to eql %w(a b c d e f)
  end

end