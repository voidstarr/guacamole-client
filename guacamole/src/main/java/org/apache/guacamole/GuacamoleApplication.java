/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.guacamole;

import com.google.inject.Injector;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.Collections;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.ApplicationPath;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * JAX-RS Application which serves as the root definition of the Guacamole
 * REST API. The HK2 dependency injection used by Jersey is automatically
 * bridged to Guice, allowing injections managed by Guice to be injected within
 * classes served by Jersey.
 */
@ApplicationPath("/*")
public class GuacamoleApplication extends ResourceConfig {

    /**
     * Creates a new GuacamoleApplication which defines the Guacamole REST API,
     * automatically configuring Jersey's HK2 dependency injection to
     * additionally pull services from a Guice injector.
     *
     * @param servletContext
     *     The ServletContext which has already associated with a Guice
     *     injector via a GuacamoleServletContextListener.
     *
     * @param serviceLocator
     *     The HK2 service locator (injector).
     */
    @Inject
    public GuacamoleApplication(ServletContext servletContext,
            ServiceLocator serviceLocator) {

        // Bridge Jersey logging (java.util.logging) to SLF4J
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        // Bridge HK2 service locator with Guice injector
        Injector guiceInjector = (Injector) servletContext.getAttribute(GuacamoleServletContextListener.GUICE_INJECTOR);
        GuiceBridge.getGuiceBridge().initializeGuiceBridge(serviceLocator);
        GuiceIntoHK2Bridge bridge = serviceLocator.getService(GuiceIntoHK2Bridge.class);
        bridge.bridgeGuiceInjector(guiceInjector);

        // Automatically scan for REST resources
        packages("org.apache.guacamole.rest");

        // Use Jackson for JSON
        register(JacksonFeature.class);

        // Configure OpenAPI/Swagger documentation
        configureOpenAPI();

    }

    /**
     * Configures OpenAPI/Swagger documentation for the REST API.
     */
    private void configureOpenAPI() {

        // Create OpenAPI definition
        OpenAPI openAPI = new OpenAPI()
            .info(new Info()
                .title("Apache Guacamole REST API")
                .version("1.6.0")
                .description("REST API for Apache Guacamole, providing programmatic access to "
                        + "connections, users, permissions, and remote desktop resources.")
                .contact(new Contact()
                    .name("Apache Guacamole")
                    .url("https://guacamole.apache.org"))
                .license(new License()
                    .name("Apache License 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0")))
            .servers(Collections.singletonList(
                new Server()
                    .url("/guacamole/api")
                    .description("Guacamole REST API")));

        // Configure Swagger to scan REST resources
        SwaggerConfiguration swaggerConfig = new SwaggerConfiguration()
            .openAPI(openAPI)
            .resourcePackages(Collections.singleton("org.apache.guacamole.rest"))
            .prettyPrint(true);

        // Register OpenAPI resource to serve openapi.json/openapi.yaml
        register(new OpenApiResource().openApiConfiguration(swaggerConfig));

    }

}
