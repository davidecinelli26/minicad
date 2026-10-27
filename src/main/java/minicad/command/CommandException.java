package minicad.command;

/** A command that cannot be carried out, with a message meant for the user. */
public class CommandException extends RuntimeException {

    public CommandException(String message) {
        super(message);
    }

    public CommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
