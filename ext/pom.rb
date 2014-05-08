project 'JRuby Ext' do

  version = File.read( File.join( basedir, '..', 'VERSION' ) ).strip

  model_version '4.0.0'
  id 'org.jruby:jruby-ext'
  inherit 'org.jruby:jruby-parent', version
  packaging 'pom'

  properties( 'tesla.dump.pom' => 'pom.xml',
              'tesla.dump.readonly' => true )

  modules [ 'openssl',
            'readline',
            'ripper' ]

  plugin( :deploy,
          'skip' =>  'true' )
end
