# -*- encoding: utf-8 -*-

# https://github.com/jruby/jruby/issues/2591
describe 'double-quoted UTF8 hash key' do
  it 'returns collect encoding' do
    h = { "Ãa1": "true" }
    expect(h.keys.first.encoding.to_s).to eq("UTF-8")
    expect(h.keys.first.to_s).to eq("Ãa1")
  end
end
