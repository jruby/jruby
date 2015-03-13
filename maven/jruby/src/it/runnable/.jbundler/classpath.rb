require 'jar_dependencies'
JBUNDLER_LOCAL_REPO = Jars.home
JBUNDLER_JRUBY_CLASSPATH = []
JBUNDLER_JRUBY_CLASSPATH.freeze
JBUNDLER_TEST_CLASSPATH = []
JBUNDLER_TEST_CLASSPATH.freeze
JBUNDLER_CLASSPATH = []
JBUNDLER_CLASSPATH << (JBUNDLER_LOCAL_REPO + '/org/bouncycastle/bcpkix-jdk15on/1.49/bcpkix-jdk15on-1.49.jar')
JBUNDLER_CLASSPATH << (JBUNDLER_LOCAL_REPO + '/org/slf4j/slf4j-simple/1.6.4/slf4j-simple-1.6.4.jar')
JBUNDLER_CLASSPATH << (JBUNDLER_LOCAL_REPO + '/org/bouncycastle/bcprov-jdk15on/1.49/bcprov-jdk15on-1.49.jar')
JBUNDLER_CLASSPATH << (JBUNDLER_LOCAL_REPO + '/org/slf4j/slf4j-api/1.6.4/slf4j-api-1.6.4.jar')
JBUNDLER_CLASSPATH.freeze
