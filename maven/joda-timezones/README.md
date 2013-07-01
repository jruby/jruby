new release
-----------

find the version of the latest release on

[ftp://ftp.iana.org/tz/releases/](ftp://ftp.iana.org/tz/releases/)

at the time of writing that was **2013c**. replace the version tag of the pom.xml and execute for local install (inside that directory)

     mvn install

or for deployment

     mvn deploy

now you need to change the version for jruby artifacts by changing the tzdata.version in the ./default.properties file
