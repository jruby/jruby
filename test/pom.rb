project 'JRuby Integration Tests' do

  version = File.read( File.join( basedir, '..', 'VERSION' ) ).strip

  model_version '4.0.0'
  id "org.jruby:jruby-tests:#{version}"
  inherit "org.jruby:jruby-parent:#{version}"
  packaging 'jar'

  repository( :id => 'rubygems-releases',
              :url => 'http://rubygems-proxy.torquebox.org/releases' )

  plugin_repository( :id => 'sonatype',
                     :url => 'https://oss.sonatype.org/content/repositories/snapshots/' ) do
    releases false
    snapshots true
  end
  plugin_repository( :id => 'rubygems-releases',
                     :url => 'http://rubygems-proxy.torquebox.org/releases' )

  properties( 'jruby.basedir' => '${basedir}/..',
              'gem.home' => '${jruby.basedir}/lib/ruby/gems/shared' )

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
       :systemPath => '${project.basedir}/requireTest-1.0.jar',
       :scope => 'system' )
  gem 'rubygems:jruby-launcher:${jruby-launcher.version}'
  gem 'rubygems:rake:${rake.version}'
  gem 'rubygems:rspec:${rspec.version}'
  gem 'rubygems:minitest:${minitest.version}'
  gem 'rubygems:minitest-excludes:${minitest-excludes.version}'
  gem 'rubygems:json:${json.version}'
  gem 'rubygems:rdoc:${rdoc.version}'

  plugin 'de.saumya.mojo:gem-maven-plugin:${jruby.plugins.version}' do
    execute_goals( 'initialize',
                   :phase => 'initialize',
                   'gemPath' =>  '${gem.home}',
                   'gemHome' =>  '${gem.home}',
                   'binDirectory' =>  '${jruby.basedir}/bin',
                   'includeRubygemsInTestResources' =>  'false',
                   'libDirectory' =>  '${jruby.basedir}/lib',
                   'jrubyJvmArgs' =>  '-Djruby.home=${jruby.basedir}' )
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


  rake = Dir[File.join(basedir, "../lib/ruby/gems/shared/gems/rake-*/lib/rake/rake_test_loader.rb")].first.sub( /.*\/..\/lib\//, 'lib/' )
  files = ""
  File.open(File.join(basedir, 'jruby.1.9.index')) do |f|
    f.each_line.each do |line|
      filename = "#{basedir}/#{line.chomp}.rb"
      next unless File.exist? filename
      filename.sub!( /.*\/test\//, 'test/' )
      files << "<arg value='#{filename}'/>\n"
    end
  end

  target1 = "<target name='test-jruby-complete-jar'>\n<exec executable='java' failonerror='true'>\n<arg value='-Djruby.home=uri:classloader://META-INF/jruby.home'/>\n<arg value='-cp'/>\n<arg value='core/target/test-classes:test/target/test-classes:maven/jruby-complete/target/jruby-complete-#{version}.jar'/>\n<arg value='org.jruby.Main'/>\n<arg value='-I.'/>\n<arg value='#{rake}'/>\n#{files}<arg value='-v'/>\n</exec>\n</target>\n"
  target2 = "<target name='test-jruby-core-stdlib-jars'>\n<exec executable='java' failonerror='true'>\n<arg value='-Djruby.home=uri:classloader://META-INF/jruby.home'/>\n<arg value='-cp'/>\n<arg value='core/target/test-classes:test/target/test-classes:lib/jruby.jar:maven/jruby-stdlib/target/jruby-stdlib-#{version}.jar'/>\n<arg value='org.jruby.Main'/>\n<arg value='-I.'/>\n<arg value='#{rake}'/>\n#{files}<arg value='-v'/>\n</exec>\n</target>\n"

  File.write(File.join(basedir, '..', 'antlib', 'extra.xml'), "<project basedir='..'>\n#{target1}#{target2}<target description='test using jruby-complete or jruby-core/jruby-stdlib jars' name='test-jruby-jars' depends='test-jruby-complete-jar,test-jruby-core-stdlib-jars'/></project>")

end
