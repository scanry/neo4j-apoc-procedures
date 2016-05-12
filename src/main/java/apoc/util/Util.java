package apoc.util;

import apoc.path.RelationshipTypeAndDirections;
import org.neo4j.graphdb.*;
import org.neo4j.helpers.collection.Iterables;
import org.neo4j.helpers.collection.Pair;
import org.neo4j.procedure.Name;

import java.util.*;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.lang.String.format;

/**
 * @author mh
 * @since 24.04.16
 */
public class Util {
    public static final Label[] NO_LABELS = new Label[0];

    public static Label[] labels(Object labelNames) {
        if (labelNames==null) return NO_LABELS;
        if (labelNames instanceof List) {
            List names = (List) labelNames;
            Label[] labels = new Label[names.size()];
            int i = 0;
            for (Object l : names) {
                if (l==null) continue;
                labels[i++] = Label.label(l.toString());
            }
            if (i <= labels.length) return Arrays.copyOf(labels,i);
            return labels;
        }
        return new Label[]{Label.label(labelNames.toString())};
    }

    public static RelationshipType type(Object type) {
        if (type == null) throw new RuntimeException("No relationship-type provided");
        return RelationshipType.withName(type.toString());
    }

    @SuppressWarnings("unchecked")
    public static LongStream ids(Object ids) {
        if (ids == null) return LongStream.empty();
        if (ids instanceof Number) return LongStream.of(((Number)ids).longValue());
        if (ids instanceof Collection) {
            Collection<Object> coll = (Collection<Object>) ids;
            return coll.stream().mapToLong( (o) -> ((Number)o).longValue());
        }
        if (ids instanceof Iterable) {
            Spliterator<Object> spliterator = ((Iterable) ids).spliterator();
            return StreamSupport.stream(spliterator,false).mapToLong( (o) -> ((Number)o).longValue());
        }
        throw new RuntimeException("Can't convert "+ids.getClass()+" to a stream of long ids");
    }

    public static Stream<Relationship> relsStream(GraphDatabaseService db, Object ids) {
        return ids(ids).parallel().mapToObj(db::getRelationshipById);
    }

    public static Stream<Node> nodeStream(GraphDatabaseService db, @Name("nodes") Object ids) {
        return ids(ids).parallel().mapToObj(db::getNodeById);
    }

    private static double doubleValue(PropertyContainer pc, String prop, Number defaultValue) {
        Object costProp = pc.getProperty(prop, defaultValue);
        if (costProp instanceof Number) {
            return ((Number) costProp).doubleValue();
        }
        return Double.parseDouble(costProp.toString());

    }

    private static double doubleValue(PropertyContainer pc, String prop) {
        return doubleValue(pc, prop, 0);
    }

    public static Direction parseDirection(String direction) {
        if (null == direction) {
            return Direction.BOTH;
        }
        try {
            return Direction.valueOf(direction.toUpperCase());
        } catch (Exception e) {
            throw new RuntimeException(format("Cannot convert value '%s' to Direction. Legal values are '%s'",
                    direction, Arrays.toString(Direction.values())));
        }
    }

    public static RelationshipType[] toRelTypes(List<String> relTypeStrings) {
        RelationshipType[] relTypes = new RelationshipType[relTypeStrings.size()];
        for (int i = 0; i < relTypes.length; i++) {
            relTypes[i] = RelationshipType.withName(relTypeStrings.get(i));
        }
        return relTypes;
    }

    public static RelationshipType[] allRelationshipTypes(GraphDatabaseService db) {
        return Iterables.asArray(RelationshipType.class, db.getAllRelationshipTypes());
    }

    public static RelationshipType[] typesAndDirectionsToTypesArray(String typesAndDirections) {
        List<RelationshipType> relationshipTypes = new ArrayList<>();
        for (Pair<RelationshipType, Direction> pair : RelationshipTypeAndDirections.parse(typesAndDirections)) {
            if (null != pair.first()) {
                relationshipTypes.add(pair.first());
            }
        }
        return relationshipTypes.toArray(new RelationshipType[relationshipTypes.size()]);
    }
}
