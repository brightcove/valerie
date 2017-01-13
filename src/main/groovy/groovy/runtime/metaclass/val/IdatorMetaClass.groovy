package groovy.runtime.metaclass.val

/**
 * Meta class for {@link val.Idator}. This is automatically used as the
 * meta class due to using conventions around package and class names.
 * This ensures some basic MOP behavior
 */
//In future versions dispatching of calls to Checkers or extension libraries are
// likely to be handled here rather than code within the val.Idator class.
class IdatorMetaClass extends ExpandoMetaClass {

    IdatorMetaClass(MetaClassRegistry registry,
                    Class theClass,
                    boolean register=true,
                    boolean allowChangesAfterInit=true,
                    MetaMethod[] add=[]) {
        super(registry, theClass, register, allowChangesAfterInit, add)
    }
}
