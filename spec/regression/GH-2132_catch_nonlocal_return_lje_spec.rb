# https://github.com/jruby/jruby/issues/2132

describe 'NonLocalReturn' do
  it 'throwing a LocalJumpError should be properly rescued' do
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
