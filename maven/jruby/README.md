# websphere libery profile #

to run websphere you need to download

<https://developer.ibm.com/wasdev/downloads/liberty-profile-using-non-eclipse-environments/>

and tell maven
```
$ mvn verify -Dwlp.jar=path/to/wlp.jar
```
where your wlp jar is located.


## permanent setup with settings.xml ##

add to $HOME/.m2/settings.xml

    <profiles>
      <profile>
        <id>wlp</id>
        <properties>
	      <wlp.jar>/path/to/wlp-developers-runtime-8.5.5.1.jar</wlp.jar>
        </properties>
      </profile>
	</profiles>
    <activeProfiles>
      <activeProfile>wlp</activeProfile>
    </activeProfiles>

so whenever you run the tests the webspere tests will run as well
