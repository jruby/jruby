project 'JRuby Artifacts' do

  version = File.read( File.join( basedir, '..', 'VERSION' ) ).strip

  model_version '4.0.0'
  id 'jruby-artifacts'
  inherit 'org.jruby:jruby-parent', version
  packaging 'pom'

  # it looks like some people have problems with this artifact as parent
  # TODO set the parent pom to pom.rb inside the children
  properties( 'polyglot.dump.pom' => 'pom.xml',
              'polyglot.dump.readonly' => true )

  plugin_management do
    plugin 'org.codehaus.mojo:build-helper-maven-plugin' do
      execute_goals( 'attach-artifact',
                     :id => 'attach-artifacts',
                     :phase => 'package',
                     'artifacts' => [ { 'file' =>  '${basedir}/src/empty.jar',
                                        'classifier' =>  'sources' },
                                      { 'file' =>  '${basedir}/src/empty.jar',
                                        'classifier' =>  'javadoc' } ] )
    end
  end

  # module to profile map
  map = { 'jruby' => [ :release, :main, :osgi, :j2ee ],
    'jruby-complete' => [ :release, :complete, :osgi, :'jruby_complete_jar_extended', :snapshots],
    'jruby-jars' => [ :release, :'jruby-jars', :snapshots ],
    'jruby-dist' => [ :release, :dist ]
  }

  profile :all do
    modules map.keys
  end

  # TODO once ruby-maven profile! we can do this in one loop
  invert = {}
  map.each do |m, pp|
    pp.each do |p|
      ( invert[ p ] ||= [] ) << m
    end
  end
  invert.each do |p, m|
    profile p do
      modules m
    end
  end
end
