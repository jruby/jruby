import java.io.InputStream;
import java.io.ByteArrayOutputStream;
public class App {

    public static void main(String... args) {
    }

    public App() {
    }

    public String bannerURL() throws Exception {
	return getClass().getResource( "banner.txt" ).toString();
    }

    public String banner() throws Exception{
	// System.err.println( getClass().getClassLoader().getResource( "banner.txt" ) );
	// System.err.println( getClass().getResource( "banner.txt" ) );
	// System.err.println( "-------------------" );
	// System.err.println( getClass().getResource( "banner.txt" ).openStream() );
	InputStream is = getClass().getClassLoader().getResourceAsStream( "banner.txt" );
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	int i = is.read();
	while( i != -1 ){
	    out.write( i );
	    i = is.read();
	}
	is.close();
	out.close();
	return out.toString();
    }
}
