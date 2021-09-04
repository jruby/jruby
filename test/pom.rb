version = ENV['JRUBY_VERSION'] ||
  File.read( File.join( basedir, '..', 'VERSION' ) ).strip

project 'JRuby Integration Tests' do

  model_version '4.0.0'

  inherit 'org.jruby:jruby-parent', version
  id 'org.jruby:jruby-tests'

  extension 'org.torquebox.mojo:mavengem-wagon:1.0.3'

  repository :id => :mavengems, :url => 'mavengem:http://rubygems.org'
  plugin_repository :id => :mavengems, :url => 'mavengem:http://rubygems.org'

  plugin_repository( :url => 'https://oss.sonatype.org/content/repositories/snapshots/',
                     :id => 'sonatype' ) do
    releases 'false'
    snapshots 'true'
  end

  properties( 'polyglot.dump.pom' => 'pom.xml',
              'jruby.home' => '${basedir}/..',
              'gem.home' => '${jruby.home}/lib/ruby/gems/shared' )

  scope :test do
    jar 'junit:junit:4.11'
    jar 'jakarta.annotation:jakarta.annotation-api:2.0.0'
    jar 'commons-logging:commons-logging:1.1.3'
    jar 'org.livetribe:livetribe-jsr223:2.0.7'
    jar 'org.jruby:jruby-core', '${project.version}'
  end
  scope :provided do
    jar 'org.apache.ant:ant:${ant.version}'
  end
  jar( 'org.jruby:requireTest:1.0',
       :scope => 'system',
       :systemPath => '${project.basedir}/jruby/requireTest-1.0.jar' )

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

  jruby_plugin :gem, '${jruby.plugins.version}' do
    options = { :phase => 'initialize',
      'gemPath' => '${gem.home}',
      'gemHome' => '${gem.home}',
      'binDirectory' => '${jruby.home}/bin',
      'includeRubygemsInTestResources' => 'false',
      'jrubyVersion' => '9.2.9.0'
    }

    execute_goals( 'initialize', options )
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
                                        { 'groupId' =>  'jakarta.annotation',
                                          'artifactId' =>  'jakarta.annotation-api',
                                          'version' =>  '2.0.0',
                                          'type' =>  'jar',
                                          'overWrite' =>  'false',
                                          'outputDirectory' =>  'target',
                                          'destFileName' =>  'annotation-api.jar' },
                                        { 'groupId' =>  'com.googlecode.jarjar',
                                          'artifactId' =>  'jarjar',
                                          'version' =>  '1.1',
                                          'type' =>  'jar',
                                          'overWrite' =>  'false',
                                          'outputDirectory' =>  'target',
                                          'destFileName' =>  'jarjar.jar' } ] )
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

  profile 'rake' do

    plugin :antrun do
      execute_goals( 'run',
                     :id => 'rake',
                     :phase => 'test',
                     :configuration => [ xml( '<target><exec dir="${jruby.home}" executable="${jruby.home}/bin/jruby" failonerror="true"><env key="JRUBY_OPTS" value=""/><arg value="-S"/><arg value="rake"/><arg value="${task}"/></exec></target>' ) ] )
    end

  end

  profile 'jruby_complete_jar_extended' do

    jar 'org.jruby:jruby-complete', '${project.version}', :scope => :provided

    plugin :antrun do
      [ 'jruby', 'objectspace', 'slow' ].each do |index|
        files = []
        File.open(File.join(basedir, index + '.index')) do |file|
          file.each_line do |line|
            next if line =~ /^#/ || line.strip.empty?
            filename = "mri/#{line.chomp}"
            filename = "jruby/#{line.chomp}.rb" unless File.exist? File.join(basedir, filename)
            filename = "#{line.chomp}.rb" unless File.exist? File.join(basedir, filename)
            next if filename =~ /mri\/psych\//
            next if filename =~ /mri\/net\/http\//
            next unless File.exist? File.join(basedir, filename)
            files << "<arg value='test/#{filename}'/>"
          end
        end
        files = files.join('')

        execute_goals( 'run',
                       :id => "jruby_complete_jar_#{index}",
                       :phase => 'test',
                       :configuration => [ xml( "<target><exec dir='${jruby.home}' executable='java' failonerror='true'><arg value='-cp'/><arg value='core/target/test-classes:test/target/test-classes:maven/jruby-complete/target/jruby-complete-${project.version}.jar'/><arg value='-Djruby.home=${jruby.home}'/><arg value='-Djruby.aot.loadClasses=true'/><arg value='org.jruby.Main'/><arg value='-I.'/><arg value='-Itest'/><arg value='lib/ruby/gems/shared/gems/rake-${rake.version}/lib/rake/rake_test_loader.rb'/>#{files}<arg value='-v'/></exec></target>" ) ] )
      end
    end

  end

end
