package fabscreen.platform.base.model;

import java.util.Objects;

public class ModuleCompact {
    public int id;
    public int index;

    public ModuleCompact(int id, int index) {
        this.id = id;
        this.index = index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModuleCompact that = (ModuleCompact) o;
        return id == that.id && index == that.index;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, index);
    }
}
