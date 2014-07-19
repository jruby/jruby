version = File.read( File.join( basedir, '..', 'VERSION' ) ).strip
project 'JRuby Integration Tests' do

  model_version '4.0.0' 

  inherit 'org.jruby:jruby-parent', version
  id 'org.jruby:jruby-tests'

  repository( 'http://rubygems-proxy.torquebox.org/releases',
              :id => 'rubygems-releases' )
  repository( 'http://rubygems-proxy.torquebox.org/prereleases',
              :id => 'rubygems-prereleases' ) do
    releases 'false'
    snapshots 'true'
  end

  plugin_repository( 'https://oss.sonatype.org/content/repositories/snapshots/',
                     :id => 'sonatype' ) do
    releases 'false'
    snapshots 'true'
  end
  plugin_repository( 'http://rubygems-proxy.torquebox.org/releases',
                     :id => 'rubygems-releases' )

  properties( 'tesla.dump.pom' => 'pom.xml',
              'tesla.dump.readonly' => true,

              'jruby.home' => '${basedir}/..',
              'gem.home' => '${jruby.home}/lib/ruby/gems/shared' )

  jar 'org.jruby:jruby-core:${project.version}'
  jar( 'junit:junit:4.11',
       :scope => 'test' )
  jar( 'org.apache.ant:ant:${ant.version}',
       :scope => 'provided' )
  jar( 'bsf:bsf:2.4.0',
       :scope => 'provided' )
  jar( 'commons-logging:commons-logging:1.1.3',
       :scope => 'test' )
  jar( 'org.livetribe:livetribe-jsr223:2.0.7',
       :scope => 'test' )
  jar( 'org.jruby:requireTest:1.0',
       :scope => 'system',
       :systemPath => '${project.basedir}/jruby/requireTest-1.0.jar' )
  gem 'rubygems:rspec:${rspec.version}'
  gem 'rubygems:minitest:${minitest.version}'
  gem 'rubygems:minitest-excludes:${minitest-excludes.version}'
  gem 'rubygems:rdoc:${rdoc.version}-SNAPSHOT'
  gem 'rubygems:json:${json.version}'
  gem 'rubygems:rake:${rake.version}'

  overrides do
    plugin( 'org.eclipse.m2e:lifecycle-mapping:1.0.0',
            'lifecycleMappingMetadata' => {
              'pluginExecutions' => [ { 'pluginExecutionFilter' => {
                                          'groupId' =>  'de.saumya.mojo',
                                          'artifactId' =>  'gem-maven-plugin',
                                          'versionRange' =>  '[1.0.0-rc3,)',
                                          'goals' => [ 'initialize' ]
                                        },
                                        'action' => {
                                          'ignore' =>  ''
                                        } } ]
            } )
  end

  plugin 'de.saumya.mojo:gem-maven-plugin:${jruby.plugins.version}' do
    execute_goals( 'initialize',
                   :phase => 'initialize',
                   'gemPath' =>  '${gem.home}',
                   'gemHome' =>  '${gem.home}',
                   'binDirectory' =>  '${jruby.home}/bin',
                   'includeRubygemsInTestResources' =>  'false',
                   'libDirectory' =>  '${jruby.home}/lib',
                   'jrubyJvmArgs' =>  '-Djruby.home=${jruby.home}' )
  end

  plugin( :compiler,
          'encoding' =>  'utf-8',
          'debug' =>  'true',
          'verbose' =>  'true',
          'fork' =>  'true',
          'showWarnings' =>  'true',
          'showDeprecation' =>  'true',
          'source' =>  '${base.java.version}',
          'target' =>  '${base.java.version}' )
  plugin :dependency do
    execute_goals( 'copy',
                   :id => 'copy jars for testing',
                   :phase => 'process-classes',
                   'artifactItems' => [ { 'groupId' =>  'junit',
                                          'artifactId' =>  'junit',
                                          'version' =>  '4.11',
                                          'type' =>  'jar',
                                          'overWrite' =>  'false',
                                          'outputDirectory' =>  'target',
                                          'destFileName' =>  'junit.jar' },
                                        { 'groupId' =>  'com.googlecode.jarjar',
                                          'artifactId' =>  'jarjar',
                                          'version' =>  '1.1',
                                          'type' =>  'jar',
                                          'overWrite' =>  'false',
                                          'outputDirectory' =>  'target',
                                          'destFileName' =>  'jarjar.jar' },
                                        { 'groupId' =>  'bsf',
                                          'artifactId' =>  'bsf',
                                          'version' =>  '2.4.0',
                                          'type' =>  'jar',
                                          'overWrite' =>  'false',
                                          'outputDirectory' =>  'target',
                                          'destFileName' =>  'bsf.jar' } ] )
  end

  plugin( :deploy,
          'skip' =>  'true' )
  plugin( :site,
          'skip' =>  'true',
          'skipDeploy' =>  'true' )

  build do
    default_goal 'test'
    test_source_directory '.'
  end

  profile 'bootstrap' do

    gem 'rubygems:jruby-launcher:${jruby-launcher.version}'

  end

  profile 'rake' do

    plugin :antrun do
      execute_goals( 'run',
                     :id => 'rake',
                     :phase => 'validate',
                     :configuration => [ xml( '<target><exec dir="${jruby.home}" executable="${jruby.home}/bin/jruby" failonerror="true"><arg value="-S"/><arg value="rake"/><arg value="${task}"/></exec></target>' ) ] )
    end

  end

  profile 'truffle' do

    plugin :antrun do
      execute_goals( 'run',
                     :id => 'rake',
                     :phase => 'validate',
                     :configuration => [ xml( '<target><exec dir="${jruby.home}" executable="${jruby.home}/bin/jruby" failonerror="true"><arg value="-X+T" /><arg value="-Xtruffle.exceptions.print_java=true" /><arg value="-J-ea" /><arg value="spec/mspec/bin/mspec" /><arg value="run" /><arg value="-t" /><arg value="bin/jruby" /><arg value="-T" /><arg value="-X+T" /><arg value="-T" /><arg value="-Xtruffle.exceptions.print_java=true" /><arg value="-T" /><arg value="-J-ea" /><arg value="--config" /> <arg value="spec/truffle/truffle.mspec" /><arg value="--excl-tag" /><arg value="fails" /></exec></target>' ) ] )
    end

  end

end
