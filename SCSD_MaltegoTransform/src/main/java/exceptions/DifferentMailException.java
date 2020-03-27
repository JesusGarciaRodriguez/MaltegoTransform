package exceptions;

public class DifferentMailException extends Exception {

    public DifferentMailException(){
        super();
    }
    public DifferentMailException(String msg){
        super(msg);
    }
}
