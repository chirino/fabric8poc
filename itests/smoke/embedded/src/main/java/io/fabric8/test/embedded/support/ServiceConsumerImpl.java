package io.fabric8.test.embedded.support;

import org.apache.felix.scr.annotations.*;

import java.util.Date;
import java.util.Map;

import static io.fabric8.test.embedded.support.Tracer.trace;

/**
* Created by chirino on 6/11/14.
*/
@Component(configurationPid = "service-consumer", policy = ConfigurationPolicy.REQUIRE, immediate = true)
@Service(ServiceConsumer.class)
public final class ServiceConsumerImpl implements ServiceConsumer {

    @Reference
    ServiceProvider provider;
    private Map<String, Object> configuration;

    @Activate
    public void activate(Map<String, Object> configuration){
        trace();
        this.configuration = configuration;
    }

    @Deactivate
    public void deactivate(){
        trace();
    }

    public String providerVersion() {
        if( provider==null )
            return null;
        return provider.providerVersion();
    }

    @Override
    public String consumerVersion() {
        if( configuration == null ) {
            return null;
        }
        return (String) configuration.get("version");
    }

    void bindProvider(ServiceProvider provider) {
        trace("provider:"+provider.providerVersion());
        this.provider = provider;
    }

    void unbindProvider(ServiceProvider provider) {
        trace("provider:"+provider.providerVersion());
        this.provider = null;
    }
}
