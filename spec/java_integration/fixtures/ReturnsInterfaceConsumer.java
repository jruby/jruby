package java_integration.fixtures;

public class ReturnsInterfaceConsumer {
    private ReturnsInterface returnsInterface;

    public ReturnsInterfaceConsumer() {}

    public ReturnsInterfaceConsumer(ReturnsInterface returnsInterface) {
        this.returnsInterface = returnsInterface;
    }

    public void setReturnsInterface(ReturnsInterface returnsInterface) {
        this.returnsInterface = returnsInterface;
    }

    public Runnable getRunnable() {
        return returnsInterface.getRunnable();
    }
}
