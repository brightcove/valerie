package val;

import java.util.*;

//TODO: This should be cleaned up after traversal is shifted
//to the map keys
public class ChildTraverser extends TransformerCheck {
    String childName;

    @Override
    public Object transform(Object input) {
        if (input == null) return null;
        if (childName == null) return input;
        if (input instanceof Map)
            return ((Map) input).get(childName);
        if (childName.equals("value") &&
            (input instanceof Map.Entry))
            return ((Map.Entry) input).getValue();
        throw new IllegalArgumentException();

    }
}
