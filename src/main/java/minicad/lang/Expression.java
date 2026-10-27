package minicad.lang;

import minicad.command.Workspace;

/**
 * A node of the syntax tree: the AbstractExpression of the Interpreter pattern. Each node knows
 * where it is in the line, and interprets itself against the workspace, its Context. Terminals
 * give values (a number, a point, a shape looked up by id); statements give the {@code Command}
 * to run. An id is checked here, before anything runs, so a wrong one is underlined in the line.
 */
public interface Expression<T> {

    T interpret(Workspace workspace);

    Span span();
}
