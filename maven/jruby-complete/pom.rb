require 'fileutils'

project 'JRuby Complete' do

  version = ENV['JRUBY_VERSION'] ||
    File.read( File.join( basedir, '..', '..', 'VERSION' ) ).strip

  model_version '4.0.0'
  id "org.jruby:jruby-complete:#{version}"
  inherit "org.jruby:jruby-artifacts:#{version}"
  packaging 'bundle'


  extension 'org.torquebox.mojo:mavengem-wagon:1.0.3'

  plugin_repository :id => :mavengems, :url => 'mavengem:https://rubygems.org'

  properties( 'polyglot.dump.pom' => 'pom.xml',
              'polyglot.dump.readonly' => true,
              'jruby.home' => '${basedir}/../..',
              'main.basedir' => '${project.parent.parent.basedir}',
              'jruby.complete.home' => '${project.build.outputDirectory}/META-INF/jruby.home' )

  scope :provided do
    jar 'org.jruby:jruby-core:${project.version}' do
      # this needs to match the Embed-Dependency on the maven-bundle-plugin
      exclusion 'com.github.jnr:jnr-ffi'
      exclusion 'me.qmx.jitescript:jitescript'
      # HACK workaround a bug in maven + ruby-dsl + felix-plugin
      ['asm', 'asm-commons', 'asm-tree', 'asm-analysis', 'asm-util' ].each do |e|
        exclusion "org.ow2.asm:#{e}"
      end
      exclusion "org.jruby:jruby-base"
    end
    jar 'org.jruby:jruby-stdlib:${project.version}'
  end

  plugin( 'org.apache.felix:maven-bundle-plugin',
          :archive => {
            :manifest => {
              :mainClass => 'org.jruby.Main'
            },
            manifestEntries: {
              'Automatic-Module-Name' => 'org.jruby.complete'
            }
          },
          :instructions => {
            'Export-Package' => 'org.jruby.*;version=${project.version}',
            'Import-Package' => '!org.jruby.*, *;resolution:=optional',
            'DynamicImport-Package' => 'javax.*',
            'Private-Package' => '*,.',
            'Bundle-Name' => 'JRuby ${project.version}',
            'Bundle-Description' => 'JRuby ${project.version} OSGi bundle',
            'Bundle-SymbolicName' => 'org.jruby.jruby',
            # the artifactId exclusion needs to match the jruby-core from above
            'Embed-Dependency' => '*;type=jar;scope=provided;inline=true;artifactId=!jnr-ffi|jitescript|jakarta.annotation-api',
            'Embed-Transitive' => true
          } ) do
    # TODO fix DSL
    @current.extensions = true
  end

  plugin( 'org.codehaus.mojo:truezip-maven-plugin', '1.2' ) do
    execute_goals(:remove,
                  phase: :package,
                  filesets: [ { directory: '${build.directory}/${project.artifactId}-${project.version}.jar',
                                includes: ['module-info.class'] } ]
                 )
  end

  plugin( :invoker )

  # we have no sources and attach the sources and javadocs from jruby-core
  # later in the build so IDE can use them
  plugin( :source, 'skipSource' =>  'true' )

  execute 'setup other osgi frameworks', :phase => 'pre-integration-test' do |ctx|
    require 'fileutils'
    source = File.join( ctx.basedir.to_pathname, 'src', 'templates', 'osgi_many_bundles_with_embedded_gems' )
    [ 'knoplerfish', 'equinox-3.6', 'equinox-3.7', 'felix-3.2', 'felix-4.4' ].each do |m|
      target = File.join( ctx.basedir.to_pathname, 'src', 'it', 'osgi_many_bundles_with_embedded_gems_' + m )
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

  plugin( 'net.ju-n.maven.plugins:checksum-maven-plugin' )

  ['sonatype-oss-release', 'snapshots'].each do |name|
    profile name do

      # use the javadocs and sources from jruby-core !!!
      phase :package do
        set = ['sources', 'javadoc' ]
        plugin :dependency do
          items = set.collect do |classifier|
            { 'groupId' =>  '${project.groupId}',
              'artifactId' =>  'jruby-core',
              'version' =>  '${project.version}',
              'classifier' =>  classifier,
              'overWrite' =>  'false',
              'outputDirectory' =>  '${project.build.directory}' }
          end
          execute_goals( 'copy',
                         :id => 'copy javadocs and sources from jruby-core',
                         'artifactItems' => items )
        end

        plugin 'org.codehaus.mojo:build-helper-maven-plugin' do
          artifacts = set.collect do |classifier|
            { 'file' =>  "${project.build.directory}/jruby-core-${project.version}-#{classifier}.jar", 'classifier' =>  classifier }
          end
          execute_goals( 'attach-artifact',
                         :id => 'attach-artifacts',
                         'artifacts' => artifacts )

          execute_goals( 'attach-artifact',
                         :id => 'attach-checksums',
                         'artifacts' => [ { file: '${project.build.directory}/jruby-complete-${project.version}.jar.sha256',
                                            type: 'jar.sha256'},
                                          { file: '${project.build.directory}/jruby-complete-${project.version}.jar.sha512',
                                            type: 'jar.sha512'} ] )

        end
      end
    end
  end

  profile :id => :jdk8 do
    activation do
      jdk '1.8'
    end
    plugin :invoker, :pomExcludes => ['osgi_many_bundles_with_embedded_gems_felix-3.2/pom.xml', '${its.j2ee}', '${its.osgi}']
  end
end
