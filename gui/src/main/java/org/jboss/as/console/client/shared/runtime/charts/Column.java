package org.jboss.as.console.client.shared.runtime.charts;

/**
 * @author Heiko Braun
 * @date 11/3/11
 */
public abstract class Column<T> {

    protected ColumnType type;
    protected String label;
    protected Column comparisonColumn = null;
    protected String deytpedName;
    protected boolean isVisible = true;
    private boolean isBaseline;

    public Column(ColumnType type, String label) {
        this.type = type;
        this.label = label;
    }

    public String getDeytpedName() {
        return deytpedName;
    }

    public void setDeytpedName(String deytpedName) {
        this.deytpedName = deytpedName;
    }

    public ColumnType getType() {
        return type;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public Column setVisible(boolean b) {
        this.isVisible = b;
        return this;
    }

    public Column setBaseline(boolean b) {
        this.isBaseline = b;
        return this;
    }

    public boolean isBaseline() {
        return isBaseline;
    }

    public String getLabel() {
        return label;
    }

    abstract T cast(String value);

    public Column getComparisonColumn() {
        return comparisonColumn;
    }

    public Column setComparisonColumn(Column comparisonColumn) {
        this.comparisonColumn = comparisonColumn;
        return this;
    }

    @Override
    public String toString() {
        return "Column{" +
                "label='" + label + '\'' +
                ", comparisonColumn=" + comparisonColumn +
                ", isBaseline=" + isBaseline +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Column column = (Column) o;

        if (isBaseline != column.isBaseline) return false;
        if (comparisonColumn != null ? !comparisonColumn.equals(column.comparisonColumn) : column.comparisonColumn != null)
            return false;
        if (!label.equals(column.label)) return false;
        if (type != column.type) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + label.hashCode();
        result = 31 * result + (comparisonColumn != null ? comparisonColumn.hashCode() : 0);
        result = 31 * result + (isBaseline ? 1 : 0);
        return result;
    }
}
