package minicad.lang;

import minicad.command.Command;
import minicad.command.Create;
import minicad.command.Delete;
import minicad.command.EditorCommands;
import minicad.command.FileCommands;
import minicad.command.GroupShapes;
import minicad.command.ListShapes;
import minicad.command.Measure;
import minicad.command.Move;
import minicad.command.Query;
import minicad.command.Restyle;
import minicad.command.Ungroup;
import minicad.command.Workspace;
import minicad.lang.Terminal.FilePath;
import minicad.lang.Terminal.Num;
import minicad.lang.Terminal.Paint;
import minicad.lang.Terminal.Pt;
import minicad.lang.Terminal.Ref;
import minicad.model.Circle;
import minicad.model.Picture;
import minicad.model.Point;
import minicad.model.Rectangle;
import minicad.model.Shape;
import minicad.model.Style;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A whole command line: the non-terminal expressions of the Interpreter. There is one record per
 * command of the language, and each interprets itself into the {@link Command} that carries it out.
 */
public sealed interface Statement extends Expression<Command> {

    /** {@code new circle|rectangle|image ... [fill C] [stroke C]} */
    record New(Expression<Shape> shape, List<Paint> paints, Span span) implements Statement {

        @Override
        public Command interpret(Workspace workspace) {
            Shape created = shape.interpret(workspace);
            created.restyle(Paint.all(paints, workspace));
            return new Create(created);
        }
    }

    record NewCircle(Num radius, Pt center, Span span) implements Expression<Shape> {

        @Override
        public Shape interpret(Workspace workspace) {
            return new Circle(center.interpret(workspace), radius.positive("the radius"), Style.OUTLINE);
        }
    }

    record NewRectangle(Pt center, Pt size, Span span) implements Expression<Shape> {

        @Override
        public Shape interpret(Workspace workspace) {
            return new Rectangle(center.interpret(workspace),
                    size.x().positive("the width"), size.y().positive("the height"), Style.OUTLINE);
        }
    }

    record NewImage(FilePath file, Pt center, Span span) implements Expression<Shape> {

        @Override
        public Shape interpret(Workspace workspace) {
            try {
                return new Picture(file.path(), workspace.images().load(file.interpret(workspace)),
                        center.interpret(workspace), 1, Style.NONE);
            } catch (IOException e) {
                throw new ScriptError(e.getMessage(), file.span());
            }
        }
    }

    /** {@code copy ID [(dx,dy)]} */
    record Copy(Ref shape, Pt offset, Span span) implements Statement {

        /** Where a copy goes when no offset is given: down and to the right, so it can be seen. */
        public static final Point DEFAULT_OFFSET = new Point(20, 20);

        @Override
        public Command interpret(Workspace workspace) {
            return new minicad.command.Copy(shape.interpret(workspace),
                    offset == null ? DEFAULT_OFFSET : offset.interpret(workspace));
        }
    }

    /** {@code del ID...} */
    record Del(List<Ref> shapes, Span span) implements Statement {

        @Override
        public Command interpret(Workspace workspace) {
            return new Delete(Ref.all(shapes, workspace));
        }
    }

    /** {@code mv ID (x,y)} */
    record Mv(Ref shape, Pt point, Span span) implements Statement {

        @Override
        public Command interpret(Workspace workspace) {
            return new Move(List.of(shape.interpret(workspace)), point.interpret(workspace), false);
        }
    }

    /** {@code mvoff ID... (dx,dy)} */
    record Mvoff(List<Ref> shapes, Pt offset, Span span) implements Statement {

        @Override
        public Command interpret(Workspace workspace) {
            return new Move(Ref.all(shapes, workspace), offset.interpret(workspace), true);
        }
    }

    /** {@code scale ID factor} */
    record Scale(Ref shape, Num factor, Span span) implements Statement {

        @Override
        public Command interpret(Workspace workspace) {
            return new minicad.command.Scale(shape.interpret(workspace), factor.positive("the scale factor"));
        }
    }

    /** {@code grp ID ID...} */
    record Grp(List<Ref> shapes, Span span) implements Statement {

        @Override
        public Command interpret(Workspace workspace) {
            return new GroupShapes(Ref.all(shapes, workspace));
        }
    }

    /** {@code ungrp ID} */
    record Ungrp(Ref group, Span span) implements Statement {

        @Override
        public Command interpret(Workspace workspace) {
            return new Ungroup(group.group(workspace));
        }
    }

    /** {@code color ID... #rrggbb} or {@code color ID... [fill C] [stroke C]} */
    record Color(List<Ref> shapes, List<Paint> paints, Span span) implements Statement {

        @Override
        public Command interpret(Workspace workspace) {
            return new Restyle(Ref.all(shapes, workspace), Paint.all(paints, workspace));
        }
    }

    /** {@code ls ...} */
    record Ls(Target target, Span span) implements Statement {

        @Override
        public Command interpret(Workspace workspace) {
            return new ListShapes(target.interpret(workspace), target.whenEmpty());
        }
    }

    /** {@code area ...} */
    record Area(Target target, Span span) implements Statement {

        @Override
        public Command interpret(Workspace workspace) {
            return new Measure(Measure.Quantity.AREA, target.interpret(workspace), target.whenEmpty());
        }
    }

    /** {@code perimeter ...} */
    record Perimeter(Target target, Span span) implements Statement {

        @Override
        public Command interpret(Workspace workspace) {
            return new Measure(Measure.Quantity.PERIMETER, target.interpret(workspace), target.whenEmpty());
        }
    }

    /** {@code save "file.mcad"} */
    record Save(FilePath file, Span span) implements Statement {

        @Override
        public Command interpret(Workspace workspace) {
            return FileCommands.save(file.withExtension(".mcad", workspace));
        }
    }

    /** {@code load "file.mcad"} */
    record Load(FilePath file, Span span) implements Statement {

        @Override
        public Command interpret(Workspace workspace) {
            return FileCommands.load(file.interpret(workspace));
        }
    }

    /** {@code export "file.svg"} */
    record Export(FilePath file, Span span) implements Statement {

        @Override
        public Command interpret(Workspace workspace) {
            return FileCommands.export(file.withExtension(".svg", workspace));
        }
    }

    record Undo(Span span) implements Statement {

        @Override
        public Command interpret(Workspace workspace) {
            return EditorCommands.undo();
        }
    }

    record Redo(Span span) implements Statement {

        @Override
        public Command interpret(Workspace workspace) {
            return EditorCommands.redo();
        }
    }

    record Clear(Span span) implements Statement {

        @Override
        public Command interpret(Workspace workspace) {
            return EditorCommands.clear();
        }
    }

    /** {@code help [command]}; {@code verb} is null for the list of all commands. */
    record Help(Verb verb, Span span) implements Statement {

        @Override
        public Command interpret(Workspace workspace) {
            if (verb != null) {
                return (Query) ws -> {
                    ws.print(verb.keyword() + ": " + verb.summary());
                    verb.usage().forEach(line -> ws.print("  " + line));
                };
            }
            int width = 11;
            String list = java.util.Arrays.stream(Verb.values())
                    .map(v -> v.keyword() + " ".repeat(width - v.keyword().length()) + v.summary())
                    .collect(Collectors.joining("\n"));
            return (Query) ws -> {
                list.lines().forEach(ws::print);
                ws.print("help NAME shows how a command is written, for example: help new");
            };
        }
    }
}
