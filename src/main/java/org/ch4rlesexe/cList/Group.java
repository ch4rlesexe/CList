package org.ch4rlesexe.cList;

public class Group {
    private final String name;
    private final String display;
    private final int priority;
    private final int max;

    public Group(String name, String display, int priority, int max) {
        this.name = name;
        this.display = display;
        this.priority = priority;
        this.max = max;
    }

    public String getName() {
        return name;
    }

    public String getDisplay() {
        return display;
    }

    public int getPriority() {
        return priority;
    }

    public int getMax() {
        return max;
    }
}
