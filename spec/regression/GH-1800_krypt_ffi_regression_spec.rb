require 'rspec'

# https://github.com/jruby/jruby/issues/1800
if RUBY_VERSION > '1.9'
  describe 'require krypt without ffi support' do
    
    let( :ffi_path ) { File.join( File.dirname( __FILE__ ), 'ffi' ) }

    it 'should not fail' do
      begin
        $LOAD_PATH.unshift ffi_path

        require( 'krypt' )

      ensure
        $LOAD_PATH.delete( ffi_path )
      end
    end
  end
end
