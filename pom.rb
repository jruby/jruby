version = ENV['JRUBY_VERSION'] ||
  File.read( File.join( basedir, 'VERSION' ) ).strip

project 'JRuby', 'https://github.com/jruby/jruby' do

  model_version '4.0.0'
  inception_year '2001'
  id 'org.jruby:jruby-parent', version
  inherit 'org.sonatype.oss:oss-parent:7'
  packaging 'pom'

  description 'JRuby is the effort to recreate the Ruby (https://www.ruby-lang.org) interpreter in Java.'

  organization 'JRuby', 'https://www.jruby.org'

  [ 'headius', 'enebo', 'wmeissner', 'BanzaiMan', 'mkristian' ].each do |name|
    developer name do
      name name
      roles 'developer'
    end
  end

  issue_management 'https://github.com/jruby/jruby/issues', 'GitHub'

  mailing_list "jruby" do
    archives "https://github.com/jruby/jruby/wiki/MailingLists"
  end

  license 'GPL-2.0', 'http://www.gnu.org/licenses/gpl-2.0-standalone.html'
  license 'LGPL-2.1', 'http://www.gnu.org/licenses/lgpl-2.1-standalone.html'
  license 'EPL-2.0', 'http://www.eclipse.org/legal/epl-v20.html'

  plugin_repository( :url => 'https://oss.sonatype.org/content/repositories/snapshots/',
                     :id => 'sonatype' ) do
    releases 'false'
    snapshots 'true'
  end
  repository( :url => 'https://oss.sonatype.org/content/repositories/snapshots/',
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
              'jruby.basedir' => '${project.basedir}',
              'main.basedir' => '${project.basedir}',
              'project.build.sourceEncoding' => 'utf-8',
              'base.java.version' => '1.8',
              'base.javac.version' => '1.8',
              'invoker.skip' => 'true',
              'version.jruby' => '${project.version}',
              'github.global.server' => 'github',
              'polyglot.dump.pom' => 'pom.xml',
              'polyglot.dump.readonly' => 'true',
              'jruby.plugins.version' => '1.0.10',

              # versions for default gems with bin executables
              # used in ./lib/pom.rb and ./maven/jruby-stdlib/pom.rb
              'rake.version' => '12.3.3',
              'jruby-launcher.version' => '1.1.6',
              'ant.version' => '1.9.8',
              'asm.version' => '9.1',
              'jffi.version' => '1.3.3',
              'joda.time.version' => '2.10.10' )

  plugin_management do
    jar( 'junit:junit:4.13.1',
         :scope => 'test' )

    plugin( 'org.apache.felix:maven-bundle-plugin:4.2.1',
            'instructions' => {
              'Export-Package' =>  'org.jruby.*;version=${project.version}',
              'Import-Package' =>  '!org.jruby.*, *;resolution:=optional',
              'Private-Package' =>  'org.jruby.*,jnr.*,com.kenai.*,com.martiansoftware.*,jay.*,jline.*,jni.*,org.fusesource.*,org.jcodings.*,org.joda.convert.*,org.joda.time.*,org.joni.*,org.yaml.*,org.yecht.*,tables.*,org.objectweb.*,com.headius.*,org.bouncycastle.*,com.jcraft.jzlib,.',
              'Bundle-Name' =>  '${bundle.name} ${project.version}',
              'Bundle-Description' =>  '${bundle.name} ${project.version} OSGi bundle',
              'Bundle-SymbolicName' =>  '${bundle.symbolic_name}'
            } ) do
      dependency(groupId: 'biz.aQute.bnd', artifactId: 'biz.aQute.bndlib', version: '4.3.1')
      execute_goals( 'manifest',
                     :phase => 'prepare-package' )
    end

    plugin( :site, '3.9.1', 'skipDeploy' =>  'true' )
    plugin 'org.codehaus.mojo:build-helper-maven-plugin:3.2.0'
    plugin 'org.codehaus.mojo:exec-maven-plugin:3.0.0'
    plugin :antrun, '3.0.0'
    plugin :source, '3.2.1'
    plugin :assembly, '3.3.0'
    plugin :install, '3.0.0-M1'
    plugin :deploy, '3.0.0-M1'
    plugin :javadoc, '3.2.0'
    plugin :resources, '3.2.0'
    plugin :clean, '3.1.0'
    plugin :dependency, '2.8'
    plugin :release, '3.0.0-M1'
    plugin :jar, '3.2.0'

    rules = { :requireMavenVersion => { :version => '[3.3.0,)' } }
    unless model.version =~ /-SNAPSHOT/
       rules[:requireReleaseDeps] = { :message => 'No Snapshots Allowed!' }
    end
    plugin :enforcer, '1.4' do
      execute_goal :enforce, :rules => rules
    end

    plugin :compiler, '3.8.1'
    plugin :shade, '3.2.4'
    plugin :surefire, '3.0.0-M2'
    plugin :plugin, '3.6.0'
    plugin( :invoker, '3.2.1',
            'properties' => { 'jruby.version' => '${project.version}',
                              'jruby.plugins.version' => '${jruby.plugins.version}' },
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
    plugin :'scm-publish', '3.1.0'
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

  modules [ 'shaded', 'core', 'lib' ]

  build do
    default_goal 'install'
  end

  profile 'test' do
    properties 'invoker.skip' => false
    modules [ 'test' ]
  end

  [ 'rake', 'exec' ].each do |name|
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

  profile 'apps' do
    modules ['maven']

    build do
      default_goal 'install'
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
    modules [ 'maven' ]
    properties 'invoker.skip' => true
    plugin(:source) do
      execute_goals('jar-no-fork', :id => 'attach-sources')
    end
    plugin(:javadoc) do
      execute_goals('jar', :id => 'attach-javadocs')
      configuration(doclint: 'none')
    end
  end

  profile 'snapshots' do

    modules [ 'maven' ]

    distribution_management do
      repository( :url => "file:${project.build.directory}/maven", :id => 'local releases' )
      snapshot_repository( :url => "file:${project.build.directory}/maven",
                           :id => 'local snapshots' )
    end
    build do
      default_goal :deploy
    end

    plugin(:source) do
      execute_goals('jar-no-fork', :id => 'attach-sources')
    end
    plugin(:javadoc) do
      execute_goals('jar', :id => 'attach-javadocs')
    end
  end

  profile 'single invoker test' do
    activation do
      property :name => 'invoker.test'
    end
    properties 'invoker.skip' => false
  end

  profile 'jdk8' do
    activation do
      jdk '1.8'
    end
    plugin :javadoc, :additionalparam => '-Xdoclint:none'
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
