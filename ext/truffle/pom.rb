id 'com.headius:openjdk-truffle:0.6'

name 'Openjdk Truffle'

jar 'com.oracle:truffle:0.6'

plugin :shade, '2.1' do
  execute_goal :shade, :phase => 'package'
end

properties 'tesla.dump.pom' => 'pom.xml'

profile :bootstrap do
  repository( 'http://lafo.ssw.uni-linz.ac.at/nexus/content/repositories/releases/',
              :id => 'truffle' ) do
    releases 'true'
    snapshots 'false'
  end
end

distribution_management do
  snapshot_repository :id => 'sonatype-nexus-snapshots', :url =>  'https://oss.sonatype.org/content/repositories/snapshots'
  repository :id => 'sonatype-nexus-staging', :url =>  'https://oss.sonatype.org/service/local/staging/deploy/maven2'
end

