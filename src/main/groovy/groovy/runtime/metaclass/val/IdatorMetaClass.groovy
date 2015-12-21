package groovy.runtime.metaclass.val

import val.Idator

class IdatorMetaClass extends ExpandoMetaClass {

    IdatorMetaClass(MetaClassRegistry registry, Class theClass, boolean register=true,
                    boolean allowChangesAfterInit=true,
                    MetaMethod[] add=[]) {
        super(registry, theClass, register, allowChangesAfterInit, add)
    }
}
