package com.workmate.domain.common;

import java.util.Objects;

/**
 * Base class for domain Entities.
 *
 * <p>An Entity has a distinct identity that runs through time; equality is based on its
 * {@link Identifier}, not its attributes (an entity can change while remaining "the same"
 * entity). Pure Java — no framework dependencies.
 *
 * @param <ID> the typed identifier of this entity
 */
public abstract class Entity<ID extends Identifier<?>> {

    private final ID id;

    protected Entity(ID id) {
        this.id = Objects.requireNonNull(id, "entity id must not be null");
    }

    public ID id() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Entity<?> other = (Entity<?>) o;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + "}";
    }
}
