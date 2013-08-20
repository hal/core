package org.useware.kernel.model.structure.builder;

import org.useware.kernel.model.behaviour.Consumer;
import org.useware.kernel.model.behaviour.Resource;
import org.useware.kernel.model.behaviour.ResourceType;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Heiko Braun
 * @date 10/31/12
 */
public class ResourceConsumption implements Consumer {

    private Set<Resource<ResourceType>> consumedTypes;

    @Override
    public Set<Resource<ResourceType>> getInputs() {
        return consumedTypes;
    }

    @Override
    public boolean doesConsume() {
        return consumedTypes!=null && !consumedTypes.isEmpty();
    }

    @Override
    public boolean doesConsume(Resource<ResourceType> resource) {
        boolean match = false;

        if(consumedTypes!=null)
        {
            for(Resource<ResourceType> candidate : consumedTypes)
            {
                if(candidate.equals(resource))
                {
                    match = true;
                    break;
                }
            }
        }
        return match;
    }

    @Override
    public void setInputs(Resource<ResourceType>... resources) {
        this.consumedTypes = new HashSet<Resource<ResourceType>>();
        for(Resource<ResourceType> event : resources)
            consumedTypes.add(event);
    }
}
