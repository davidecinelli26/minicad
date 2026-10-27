package minicad.command;

/**
 * A command that only reads: {@code ls}, {@code area}, {@code save}, {@code undo} itself. It is
 * never recorded, so undo after {@code ls} undoes the last edit, not the listing.
 */
@FunctionalInterface
public interface Query extends Command {

    @Override
    default boolean isEdit() {
        return false;
    }
}
