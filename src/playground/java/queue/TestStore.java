package queue;

/**
 * Created by daniel on 27/04/17.
 */
public class TestStore {
    String s;
    int i;

    public TestStore(String s, int i) {
        this.s = s;
        this.i = i;
    }

    public String getS() {
        return s;
    }

    public void setS(String s) {
        this.s = s;
    }

    public int getI() {
        return i;
    }

    public void setI(int i) {
        this.i = i;
    }

    @Override
    public String toString() {
        return "TestStore{" +
                "s='" + s + '\'' +
                ", i=" + i +
                '}';
    }
}
