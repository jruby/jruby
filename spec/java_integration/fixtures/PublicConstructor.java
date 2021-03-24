package java_integration.fixtures;

public class PublicConstructor {

  public int i;
  public Object v;

  public PublicConstructor() {
  }

  public PublicConstructor(int i) {
    this.i = i;
  }

  public PublicConstructor(Object v) {
    this.v = v;
  }

  public PublicConstructor(Object i, Object v) {
    this.i = ((Long) i).intValue();
    this.v = v;
  }

  public Class vClass() {
    return v.getClass();
  }
}