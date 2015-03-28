version = File.read( File.join( basedir, 'VERSION' ) ).strip
project 'JRuby', 'https://github.com/jruby/jruby' do

  model_version '4.0.0'
  inception_year '2001'
  id 'org.jruby:jruby-parent', version
  inherit 'org.sonatype.oss:oss-parent:7'
  packaging 'pom'

  description 'JRuby is the effort to recreate the Ruby (http://www.ruby-lang.org) interpreter in Java.'

  organization 'JRuby', 'http://jruby.org'

  [ 'headius', 'enebo', 'wmeissner', 'BanzaiMan', 'mkristian' ].each do |name|
    developer name do
      name name
      roles 'developer'
    end
  end

  issue_management 'https://github.com/jruby/jruby/issues', 'GitHub'

  [ 'user', 'dev', 'scm', 'annouce' ].each do |id|
    mailing_list "jruby-#{id}" do
      archives "http://markmail.org/search/list:org.codehaus.jruby.#{id}"
    end
  end

  license 'GPL 3', 'http://www.gnu.org/licenses/gpl-3.0-standalone.html'
  license 'LGPL 3', 'http://www.gnu.org/licenses/lgpl-3.0-standalone.html'
  license 'EPL', 'http://www.eclipse.org/legal/epl-v10.html'

  plugin_repository( 'https://oss.sonatype.org/content/repositories/snapshots/',
                     :id => 'sonatype' ) do
    releases 'false'
    snapshots 'true'
  end
  repository( 'https://oss.sonatype.org/content/repositories/snapshots/',
              :id => 'sonatype' ) do
    releases 'false'
    snapshots 'true'
  end

  source_control( :url => 'https://github.com/jruby/jruby',
                  :connection => 'scm:git:git@jruby.org:jruby.git',
                  :developer_connection => 'scm:git:ssh://git@jruby.org/jruby.git' )

  distribution do
    site( :url => 'https://github.com/jruby/jruby',
          :id => 'gh-pages',
          :name => 'JRuby Site' )
  end

  properties( 'its.j2ee' => 'j2ee*/pom.xml',
              'its.osgi' => 'osgi*/pom.xml',
              'tesla.version' => '0.1.1',
              'rspec-core.version' => '2.14.2',
              'jruby.basedir' => '${project.basedir}',
              'minitest.version' => '5.4.1',
              'ant.version' => '1.9.2',
              'diff-lcs.version' => '1.1.3',
              'jffi.version' => '1.2.8',
              'rake.version' => '10.1.0',
              'project.build.sourceEncoding' => 'utf-8',
              'jruby-launcher.version' => '1.1.1',
              'asm.version' => '5.0.3',
              'rspec-expectations.version' => '2.14.0',
              'base.javac.version' => '1.7',
              'krypt.version' => '0.0.2.rc1',
              'rdoc.version' => '4.1.0',
              'tesla.dump.pom' => 'pom.xml',
              'rspec.version' => '2.14.1',
              'base.java.version' => '1.7',
              'tesla.dump.readonly' => 'true',
              'rspec-mocks.version' => '2.14.1',
              'jruby.plugins.version' => '1.0.7',
              'invoker.skip' => 'true',
              'json.version' => '1.8.0',
              'version.jruby' => '${project.version}',
              'bouncy-castle.version' => '1.47',
              'github.global.server' => 'github',
              'main.basedir' => '${project.basedir}',
              'joda.time.version' => '2.5',
              'test-unit.version' => '3.0.3',
              'power_assert.version' => '0.2.3' )

  unless version =~ /-SNAPSHOT/
    properties 'jruby.home' => '${basedir}/..'
  end

  modules [ 'truffle', 'core', 'lib' ]

  plugin_management do
    jar( 'junit:junit:4.11',
         :scope => 'test' )

    plugin( 'org.apache.felix:maven-bundle-plugin:2.4.0',
            'instructions' => {
              'Export-Package' =>  'org.jruby.*;version=${project.version}',
              'Import-Package' =>  '!org.jruby.*, *;resolution:=optional',
              'Private-Package' =>  'org.jruby.*,jnr.*,com.kenai.*,com.martiansoftware.*,jay.*,jline.*,jni.*,org.fusesource.*,org.jcodings.*,org.joda.convert.*,org.joda.time.*,org.joni.*,org.yaml.*,org.yecht.*,tables.*,org.objectweb.*,com.headius.*,org.bouncycastle.*,com.jcraft.jzlib,.',
              'Bundle-Name' =>  '${bundle.name} ${project.version}',
              'Bundle-Description' =>  '${bundle.name} ${project.version} OSGi bundle',
              'Bundle-SymbolicName' =>  '${bundle.symbolic_name}'
            } ) do
      execute_goals( 'manifest',
                     :phase => 'prepare-package' )
    end

    plugin( :site, '3.3', 'skipDeploy' =>  'true' )
    plugin 'org.codehaus.mojo:build-helper-maven-plugin:1.8'
    plugin 'org.codehaus.mojo:exec-maven-plugin:1.2.1'
    plugin :antrun, '1.7'
    plugin :source, '2.1.2'
    plugin :assembly, '2.4'
    plugin :install, '2.4'
    plugin :deploy, '2.7'
    plugin :resources, '2.6'
    plugin :clean, '2.5'
    plugin :dependency, '2.8'
    plugin :release, '2.4.1'
    plugin :jar, '2.4' do
      jar 'org.codehaus.plexus:plexus-io:2.0.5'
    end

    plugin :compiler, '3.1'
    plugin :shade, '2.1'
    plugin :surefire, '2.15'
    plugin :plugin, '3.2'
    plugin( :invoker, '1.8',
            'settingsFile' =>  '${basedir}/src/it/settings.xml',
            'localRepositoryPath' =>  '${project.build.directory}/local-repo',
            'properties' => { 'project.version' => '${project.version}' },
            'pomIncludes' => [ '*/pom.xml' ],
            'pomExcludes' => [ 'extended/pom.xml', '${its.j2ee}', '${its.osgi}' ],
            'projectsDirectory' =>  'src/it',
            'cloneProjectsTo' =>  '${project.build.directory}/it',
            'preBuildHookScript' =>  'setup.bsh',
            'postBuildHookScript' =>  'verify.bsh',
            'goals' => [:install],
            'streamLogs' =>  'true' ) do
      execute_goals( 'install', 'run',
                     :id => 'integration-test' )
    end

    plugin 'org.eclipse.m2e:lifecycle-mapping:1.0.0'
    plugin :'scm-publish', '1.0-beta-2'
  end

  plugin( :site,
          'port' =>  '9000',
          'tempWebappDirectory' =>  '${basedir}/target/site/tempdir' ) do
    execute_goals( 'stage',
                   :id => 'stage-for-scm-publish',
                   :phase => 'post-site',
                   'skipDeploy' =>  'false' )
  end

  plugin( :'scm-publish', '1.0-beta-2',
          'scmBranch' =>  'gh-pages',
          'pubScmUrl' =>  'scm:git:git@github.com:jruby/jruby.git',
          'tryUpdate' =>  'true' ) do
    execute_goals( 'publish-scm',
                   :id => 'scm-publish',
                   :phase => 'site-deploy' )
  end


  build do
    default_goal 'install'
  end

  profile 'test' do
    properties 'invoker.skip' => false
    modules [ 'test' ]
  end

  [
    'rake', 'exec', 'truffle-specs-language', 'truffle-specs-core',
    'truffle-specs-library', 'truffle-specs-language-report',
    'truffle-specs-core-report', 'truffle-specs-library-report', 'truffle-test-pe'
  ].each do |name|
    profile name do

      modules [ 'test' ]

      build do
        default_goal 'package'
      end
    end
  end

  [ 'bootstrap', 'bootstrap-no-launcher' ].each do |name|
    profile name do

      modules [ 'test' ]

    end
  end

  [ 'jruby-jars', 'main', 'complete', 'dist' ].each do |name|

    profile name do

      modules [ 'maven' ]

      build do
        default_goal 'install'
	plugin_management do
          plugin :surefire, '2.15', :skipTests => true
        end
      end
    end
  end

  [ 'osgi', 'j2ee' ].each do |name|
    profile name do

      modules [ 'maven' ]
    
      properties( 'invoker.skip' => false,
                  "its.#{name}" => 'no-excludes/pom.xml' )
      
      build do
        default_goal 'install'
        plugin :invoker, 'pomIncludes' => [ "#{name}*/pom.xml" ]
      end
    end
  end

  profile 'jruby_complete_jar_extended' do

    modules [ 'test', 'maven' ]

    build do
      default_goal 'install'
    end
  end

  all_modules = [ 'test', 'maven' ]

  profile 'all' do

    modules all_modules

    build do
      default_goal 'install'
    end
  end

  profile 'clean' do

    modules all_modules

    build do
      default_goal 'clean'
    end
  end

  profile 'release' do
    modules [ 'test', 'maven' ]
    properties 'invoker.skip' => true
  end

  profile 'snapshots' do
    snapshots_dir = '/builds/snapshots'

    properties 'snapshots.dir' => snapshots_dir
    activation do
      file( :exists => snapshots_dir )
    end

    distribution_management do
      repository( "file:#{snapshots_dir}/maven", :id => 'local releases' )
      snapshot_repository( "file:#{snapshots_dir}/maven",
                           :id => 'local snapshots' )
    end
    build do
      default_goal :deploy
    end
  end

  profile 'single invoker test' do
    activation do
      property :name => 'invoker.test'
    end
    properties 'invoker.skip' => false
  end

  reporting do
    plugin( :'project-info-reports', '2.4',
            'dependencyLocationsEnabled' =>  'false',
            'dependencyDetailsEnabled' =>  'false' )
    plugin :changelog, '2.2'
    plugin( :checkstyle, '2.9.1',
            'configLocation' =>  '${main.basedir}/docs/style_checks.xml',
            'propertyExpansion' =>  'cacheFile=${project.build.directory}/checkstyle-cachefile' ) do
      report_set( 'checkstyle',
                  :inherited => 'false' )
    end

    plugin( 'org.codehaus.mojo:cobertura-maven-plugin:2.5.1',
            'aggregate' =>  'true' )
    plugin :dependency, '2.8' do
      report_set 'analyze-report'
    end

    plugin 'org.codehaus.mojo:findbugs-maven-plugin:2.5'
    plugin( :javadoc, '2.9',
            'quiet' =>  'true',
            'aggregate' =>  'true',
            'failOnError' =>  'false',
            'detectOfflineLinks' =>  'false',
            'show' =>  'package',
            'level' =>  'package',
            'maxmemory' =>  '1g' ) do
      report_set( 'javadoc',
                  'quiet' =>  'true',
                  'failOnError' =>  'false',
                  'detectOfflineLinks' =>  'false' )
    end

    plugin( :pmd, '2.7.1',
            'linkXRef' =>  'true',
            'sourceEncoding' =>  'utf-8',
            'minimumTokens' =>  '100',
            'targetJdk' =>  '${base.javac.version}' )
    plugin( :jxr, '2.3',
            'linkJavadoc' =>  'true',
            'aggregate' =>  'true' )
    plugin :'surefire-report', '2.14.1'
    plugin( 'org.codehaus.mojo:taglist-maven-plugin:2.4',
            'tagListOptions' => {
              'tagClasses' => {
                'tagClass' => {
                  'tags' => [ { 'matchString' =>  'todo',
                                'matchType' =>  'ignoreCase' },
                              { 'matchString' =>  'FIXME',
                                'matchType' =>  'ignoreCase' },
                              { 'matchString' =>  'deprecated',
                                'matchType' =>  'ignoreCase' } ]
                }
              }
            } )
    plugin 'org.codehaus.mojo:versions-maven-plugin:2.1' do
      report_set 'dependency-updates-report', 'plugin-updates-report', 'property-updates-report'
    end
  end
end
