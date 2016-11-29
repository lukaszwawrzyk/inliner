public class Child extends Simple {
    public Child(int value) {
        super(value);
    }

    public int getSomething() {
        return this.value * 4;
    }
}