describe 'Enumerable#each_with_object' do

  it 'reports argument error when no arguments given' do
    begin
      [].each_with_object
      fail 'ArgumentError not raised!'
    rescue ArgumentError => e
      #expect( e.message ).to eql 'wrong number of arguments (0 for 1)'
    end
  end

end