package minicad.command;

import minicad.model.Group;

/** {@code ungrp}: dissolves a group; its members stay where they are. */
public record Ungroup(Group group) implements Command {

    @Override
    public void execute(Workspace workspace) {
        workspace.drawing().ungroup(group);
    }
}
