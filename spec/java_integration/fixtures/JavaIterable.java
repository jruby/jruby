import java.util.Iterator;
import java.util.ArrayList;

public class JavaIterable implements Iterable<String>
{
    public JavaIterable(Iterable<String> strings)
    {
        _strings = new ArrayList<String>();
        for(String string : strings) {
            _strings.add(string);
        }
    }
    
    public Iterator<String> iterator()
    {
        return _strings.iterator();
    }

    private final ArrayList<String> _strings;
}
