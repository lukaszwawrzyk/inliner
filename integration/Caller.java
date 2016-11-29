class Caller {
    public static void main(String... args) {
        Simple simple = new Simple();
        simple.incCounter();
        simple.incCounter();
        int counter = simple.getCounter();
        System.out.println(counter);

        if (simple instanceof Simple) {
            return;
        }
        System.out.println("xx");
    }
}