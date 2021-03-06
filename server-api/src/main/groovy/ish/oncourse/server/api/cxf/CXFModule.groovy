/*
 * Copyright ish group pty ltd 2020.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 */

package ish.oncourse.server.api.cxf

import com.google.inject.Binder
import com.google.inject.Key
import com.google.inject.Provides
import com.google.inject.Singleton
import com.google.inject.multibindings.Multibinder
import io.bootique.ConfigModule
import io.bootique.config.ConfigurationFactory
import io.bootique.jetty.JettyModule
import io.bootique.jetty.MappedServlet
import org.apache.cxf.interceptor.LoggingInInterceptor
import org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet

import javax.ws.rs.core.Application

class CXFModule extends ConfigModule {

    static Multibinder<Object> contributeResources(Binder binder) {
        return Multibinder.newSetBinder(binder, Key.get(Object.class, CXFResource))
    }

    static Multibinder<Object> contributeFeatures(Binder binder) {
        return Multibinder.newSetBinder(binder, Key.get(Object.class, CXFFeature))
    }

    @Override
    void configure(Binder binder) {
        JettyModule.extend(binder).addMappedServlet(Key.get(MappedServlet, CXFServlet))
        contributeResources(binder).addBinding().to(CXFDefaultService)
    }

    @CXFServlet
    @Singleton
    @Provides
    private MappedServlet createCXFServlet(CXFModuleConfig config, Application application) {
        CXFNonSpringJaxrsServlet servlet = new CXFNonSpringJaxrsServlet(application)
        return new MappedServlet(servlet, Collections.singleton(config.urlPattern), CXFServlet.name)
    }

    @Singleton
    @Provides
    private CXFModuleConfig createCXFModuleConfig(ConfigurationFactory configFactory) {
        return configFactory.config(CXFModuleConfig, configPrefix)
    }

    @Singleton
    @Provides
    private Application createApplication(@CXFResource Set<Object> resources, @CXFFeature Set<Object> features) {
        Map<String, String> props = new HashMap<>()
        props.put('jaxrs.inInterceptors', LoggingInInterceptor.class.name)

        return new CXFApplication(resources,features, props)
    }

}
