package org.jboss.as.console.client.tools.modelling.workbench.repository.vfs;

/**
 * @author Heiko Braun
 * @date 10/11/13
 */
public class Entry {
    public static final Entry ROOT = new Entry("/", Type.DIR);

    public enum Type {DIR, FILE}

    private Type type;
    private String name;
    private String link;

    public Entry(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    public Entry(Type type, String name, String link) {
        this.type = type;
        this.name = name;
        this.link = link;
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getLink() {
        return link;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Entry entry = (Entry) o;

        if (!name.equals(entry.name)) return false;
        if (type != entry.type) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }
}
