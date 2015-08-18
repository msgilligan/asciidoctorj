package org.asciidoctor.converter;

import org.asciidoctor.internal.AsciidoctorModule;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyString;

import java.util.HashMap;
import java.util.Map;

public class JavaConverterRegistry {

    private AsciidoctorModule asciidoctorModule;
    private Ruby rubyRuntime;

    public JavaConverterRegistry(AsciidoctorModule asciidoctorModule, Ruby rubyRuntime) {
        super();
        this.asciidoctorModule = asciidoctorModule;
        this.rubyRuntime = rubyRuntime;
    }

    public <U, T  extends Converter<U> & OutputFormatWriter<U>> void register(final Class<T> converterClass, String... backends) {

        RubyClass clazz = ConverterProxy.register(rubyRuntime, converterClass);

        ConverterFor converterForAnnotation = converterClass.getAnnotation(ConverterFor.class);
        if (converterForAnnotation != null) {
            // Backend annotation present => Register with name given in annotation
            String backend = !ConverterFor.UNDEFINED.equals(converterForAnnotation.format()) ? converterForAnnotation.format() : converterForAnnotation.value();
            this.asciidoctorModule.register_converter(clazz, new String[] { backend });
        } else if (backends.length == 0) {
            // No backend annotation and no backend defined => register as default backend
            this.asciidoctorModule.register_converter(clazz);
        }
        if (backends.length > 0) {
            // Always additionally register with names passed to this method
            this.asciidoctorModule.register_converter(clazz, backends);
        }
    }

    public Class<?> resolve(String backend) {
        RubyClass rubyClass = this.asciidoctorModule.resolve_converter(backend);
        Class<?> clazz = rubyClass.getReifiedClass();
        if (clazz != null) {
            return clazz;
        } else if (rubyClass.getAllocator() instanceof ConverterProxy.Allocator) {
            ConverterProxy.Allocator allocator = (ConverterProxy.Allocator) rubyClass.getAllocator();
            return allocator.getConverterClass();
        }
        return null;
    }

    public void unregisterAll() {
        this.asciidoctorModule.unregister_all_converters();
    }

    public Map<String, Class<?>> converters() {
        RubyArray rubyKeys = this.asciidoctorModule.converters();

        Map<String, Class<?>> converters = new HashMap<String, Class<?>>();
        for (Object rubyBackend : rubyKeys.getList()) {
            String backend = ((RubyString) rubyBackend).asJavaString();
            converters.put(backend, resolve(backend));
        }
        return converters;
    }
}