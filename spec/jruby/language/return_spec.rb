describe 'return' do
  it 'should flow through what we expect (GH-2132)' do
    ret = [];
    Thread.new {
      begin
        begin
          return
        rescue StandardError => e
          ret << "LJE"
        end
        ret << "out"
      ensure
        ret << "ensured"
      end
    }.join
    expect(ret).to eq(["LJE", "out", "ensured"])
  end
end
