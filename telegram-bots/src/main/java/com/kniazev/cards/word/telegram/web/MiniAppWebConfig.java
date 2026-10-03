package com.kniazev.cards.word.telegram.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves the static Mini App page at the clean URL {@code /miniapp/} (and {@code /miniapp})
 * by forwarding to {@code /miniapp/index.html}, since Spring's static resource handler does
 * not auto-resolve a directory index for non-root paths.
 *
 * <p>Only registered when an embedded servlet web server is running.
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class MiniAppWebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/miniapp").setViewName("forward:/miniapp/index.html");
        registry.addViewController("/miniapp/").setViewName("forward:/miniapp/index.html");
    }
}
