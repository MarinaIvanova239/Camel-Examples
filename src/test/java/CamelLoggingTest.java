import org.apache.camel.Component;
import org.apache.camel.Consumer;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultConsumer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.DefaultProducer;
import org.apache.camel.impl.UriEndpointComponent;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.Map;

import static org.apache.camel.LoggingLevel.INFO;
import static org.slf4j.LoggerFactory.getLogger;

public class CamelLoggingTest extends CamelTestSupport {

    private static final Logger LOG = getLogger(CamelLoggingTest.class);
    private Exchange exchange;

    public String isMockEndpoints() {
        return "mock:e";
    }

    @Before
    public void camelSetUp() throws Exception {
        context().addComponent("custom", new CamelLoggingTest.ComponentUnderTest());
        exchange = new DefaultExchange(context());
    }

    @Test
    public void shouldLogAll() throws Exception {
        // given
        context().addRoutes(routeWithException());
        // when
        template.send("direct:a", exchange);
        // then
        // check logs
    }

    private RouteBuilder routeWithException() throws Exception {
        RouteBuilder routeBuilder = new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                onException(Exception.class)
                        .continued(true);

                from("direct:a")
                        .to("direct:b")
                        .log(INFO, LOG, "Mock a executed!");

                from("direct:b")
                        .process(newRequestProcessor())
                        .log(INFO, LOG, "Direct b executed!");

                from("custom:c")
                        // exception here
                        .log(INFO, LOG, "Custom c executed!");

                from("direct:d")
                        .log(INFO, LOG, "Direct d executed") // not logged
                        .log(INFO, LOG, "Scenario finished") // logged
                        .to("mock:e");
            }
        };
        return routeBuilder;
    }

    private Processor newRequestProcessor() {
        return exchange -> {
            template.send("custom:c", exchange);
            template.send("direct:d", exchange);
        };
    }

    static class ComponentUnderTest extends UriEndpointComponent {

        ComponentUnderTest() {
            super(CamelLoggingTest.EndpointUnderTest.class);
        }

        @Override
        protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
            Endpoint endpoint = new CamelLoggingTest.EndpointUnderTest(uri, this);
            setProperties(endpoint, parameters);
            return endpoint;
        }
    }

    @UriEndpoint(scheme = "custom", syntax = "custom:name", consumerClass = DefaultConsumer.class, title = "Endpoint Under Test")
    static class EndpointUnderTest extends DefaultEndpoint {

        @UriPath
        @Metadata(required = "true")
        private String name;

        @UriParam
        private String param;

        EndpointUnderTest(String uri, Component component) {
            super(uri, component);
        }

        @Override
        public Consumer createConsumer(Processor processor) throws Exception {
            return new DefaultConsumer(this, processor) {
                // empty
            };
        }

        @Override
        public Producer createProducer() throws Exception {
            return new DefaultProducer(this) {
                @Override
                public void process(Exchange exchange) throws Exception {
                    LOG.info("Inside producer!");
                    throw new Exception();
                }
            };
        }

        @Override
        public boolean isSingleton() {
            return true;
        }

        @Override
        public boolean isLenientProperties() {
            return true;
        }
    }
}