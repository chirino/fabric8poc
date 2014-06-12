package io.fabric8.test.embedded.support;

import org.apache.felix.scr.annotations.*;

import java.util.Map;

import static io.fabric8.test.embedded.support.Tracer.trace;

/**
* Created by chirino on 6/11/14.
*/
@Component(
        configurationPid = "service-provider", policy = ConfigurationPolicy.REQUIRE, immediate = true
)
@Service(ServiceProvider.class)
public final class ServiceProviderImpl implements ServiceProvider {

    private Map<String, Object> configuration;

    @Activate
    public void activate(Map<String, Object> configuration){
        this.configuration = configuration;
        trace();
    }

    @Deactivate
    public void deactivate(){
        trace();
    }

    @Override
    public String providerVersion() {
        if( configuration == null ) {
            return null;
        }
        return (String) configuration.get("version");
    }
}
