package signifiers;

import java.util.Objects;
import jason.asSyntax.Term;

public class Action {
    Term name;
    Term resource;

    /* Constructor */
    public Action(Term name, Term resource){
        this.name = name;
        this.resource = resource;
    }

    /* Sets */
    public void setName(Term newName){
        name = newName;
    }

    public void setResource(Term newResource){
        resource = newResource;
    }

    /* Gets */
    public Term getName(){
        return name;
    }

    public Term getResource(){
        return resource;
    }

    /* Equals */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof Action)) {
            return false;
        }

        Action other = (Action) obj;

        return name.equals(other.name) && resource.equals(other.resource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, resource);
    }

    @Override
    public String toString() {
        return resource.toString() + "." + name.toString();
    }
}