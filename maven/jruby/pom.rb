project 'JRuby Main Maven Artifact' do

  version = File.read( File.join( basedir, '..', '..', 'VERSION' ) ).strip

  model_version '4.0.0'
  id "org.jruby:jruby:#{version}"
  inherit "org.jruby:jruby-artifacts:#{version}"

  # keep it a jar even without sources - easier to add to a project
  packaging 'jar'

  properties( 'tesla.dump.pom' => 'pom.xml',
              'tesla.dump.readonly' => true,
              'main.basedir' => '${project.parent.parent.basedir}' )

  jar 'org.jruby:jruby-core:${project.version}'
  jar 'org.jruby:jruby-stdlib:${project.version}'

  # we have no sources and attach an empty jar later in the build to
  # satisfy oss.sonatype.org upload
  plugin( :source, 'skipSource' =>  'true' )

  # this plugin is configured to attach empty jars for sources and javadocs
  plugin( 'org.codehaus.mojo:build-helper-maven-plugin' )

  plugin( :invoker )

  execute 'setup other osgi frameworks', :phase => 'pre-integration-test' do |ctx|
    source = File.join( ctx.basedir.to_pathname, 'src', 'templates', 'osgi_all_inclusive' )
     [ 'knoplerfish', 'equinox-3.6', 'equinox-3.7', 'felix-3.2', 'felix-4.4'].each do |m|
      target = File.join( ctx.basedir.to_pathname, 'src', 'it', 'osgi_all_inclusive_' + m )
      FileUtils.rm_rf( target )
      FileUtils.cp_r( source, target )
      File.open( File.join( target, 'invoker.properties' ), 'w' ) do |f|
        f.puts 'invoker.profiles = ' + m
      end
    end
  end

  plugin( :clean ) do
    execute_goals( :clean,
                   :phase => :clean,
                   :id => 'clean-extra-osgi-ITs',
                   :filesets => [ { :directory => '${basedir}/src/it',
                                    :includes => ['osgi*/**'] } ],
                   :failOnError => false )
  end

  profile :id => :jdk8 do
    activation do
      jdk '1.8'
    end
    plugin :invoker, :pomExcludes => ['extended/pom.xml', 'osgi_all_inclusive_felix-3.2/pom.xml', '${its.j2ee}', '${its.osgi}']
  end

  profile :id => :wlp do
    activation do
      property :name => 'wlp.jar'
    end
    execute :install_wlp, :phase => :'pre-integration-test' do |ctx|
      wlp = ctx.project.properties[ 'wlp.jar' ] || java.lang.System.properties[ 'wlp.jar' ]
      system( 'java -jar ' + wlp.to_pathname + ' --acceptLicense ' + ctx.project.build.directory.to_pathname )
      system( File.join( ctx.project.build.directory.to_pathname,
                         'wlp/bin/server' ) + 'create testing' )
      FileUtils.cp_r( File.join( ctx.basedir.to_pathname, 'src/templates/j2ee_wlp'), File.join( ctx.basedir.to_pathname, 'src/it' ) )
    end
  end
end
