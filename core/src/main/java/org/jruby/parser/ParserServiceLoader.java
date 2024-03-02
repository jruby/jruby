package org.jruby.parser;

import java.util.ServiceLoader;

public class ParserServiceLoader {
    private static final String PRISM_PROVIDER = "org.jruby.prism.ParserProviderPrism";

    public static ParserProvider defaultProvider() {
        return new ParserProviderDefault();
    }

    // Only attempt to load from prism if requested to save on CL service load overhead.
    public static ParserProvider provider(boolean loadPrism) {
        return loadPrism ? provider(PRISM_PROVIDER) : defaultProvider();
    }

    public static ParserProvider provider(String providerName) {
        ServiceLoader<ParserProvider> loader = ServiceLoader.load(ParserProvider.class);

        // FIXME: When JRuby 10 is more happening this will load potentially prism and
        //   prismwasm both as providers so things will need to be refactored a bit.
        for (ParserProvider provider: loader) {
            if (providerName.equals(provider.getClass().getName())) {
                return provider;
            }
        }

        System.out.println("Failed to load prism: Using default parser provider");
        return new ParserProviderDefault();
    }
}
