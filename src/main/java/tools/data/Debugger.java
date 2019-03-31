
package tools.data;

public class Debugger {
    private boolean debug = true;

    public void on() {
        debug = true;
    }

    public void off() {
        debug = false;
    }

    public void debug(Object[][] objects) {
        for (Object[] object : objects) {
            System.out.println(String.format("%s = %s", object[0], object[1]));
        }
    }
}
