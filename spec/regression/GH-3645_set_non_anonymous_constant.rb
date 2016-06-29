module GH3645
  class << self

    def add_mod(name) # call this from autoloaded script
      mod = Module.new { extend GH3645 }
      const_set(name, mod)
      mod
    end

  end
  autoload :S3, File.expand_path('GH-3645_autoload', File.dirname(__FILE__))
end

describe 'GH-3645' do

  it 'sets a constant' do
    GH3645.add_mod :A_MODULE
    expect( GH3645::A_MODULE.name ).to eql 'GH3645::A_MODULE'
  end

  it 'sets an auto-loaded constant' do
    expect( GH3645::S3.name ).to eql 'GH3645::S3'
  end

end