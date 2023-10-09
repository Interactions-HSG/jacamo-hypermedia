package signifiers;

import java.util.Objects;
import jason.asSyntax.Term;

public class Ability {
    Term name;

    /* Constructor */
    public Ability(Term name){
        this.name = name;
    }

    /* Sets */
    public void setName(Term newName){
        name = newName;
    }

    /* Gets */
    public Term getName(){
        return name;
    }

    /* Equals */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof Ability)) {
            return false;
        }

        Ability other = (Ability) obj;

        return name.equals(other.name);
        // return name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name.toString();
    }
}