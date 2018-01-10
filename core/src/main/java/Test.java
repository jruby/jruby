import org.jruby.Ruby;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import java.io.IOException;

public class Test {
  public static void main(String[] args) throws InterruptedException, ScriptException, ClassNotFoundException, IOException {
    for (int i = 0; i < 100; i++) blah();

    System.gc();
    System.gc();
    Thread.sleep(5000);

    System.out.println(Ruby.getGlobalRuntime().getFilenoUtil().getNumberOfWrappers());

  }

  private static void blah() throws ScriptException {
    ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
    final ScriptEngine engine = scriptEngineManager.getEngineByName("jruby");
    final ScriptContext scriptContext = new SimpleScriptContext();
    scriptContext.setAttribute("org.jruby.embed.termination", true, ScriptContext.ENGINE_SCOPE);

    engine.eval("print 'test\n'", scriptContext);
  }
}
