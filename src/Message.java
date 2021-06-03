public class Message {
    private int value;

    public Message(){
        value = 0;
    }

    public void increment(int val){
        this.value += val;
    }

    public int check(){
        return this.value;
    }
}
