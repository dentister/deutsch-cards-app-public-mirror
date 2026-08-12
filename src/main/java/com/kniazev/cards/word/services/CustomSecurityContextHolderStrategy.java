package com.kniazev.cards.word.services;

import java.util.Optional;

import org.apache.catalina.session.StandardSession;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import com.vaadin.flow.server.VaadinSession;

public class CustomSecurityContextHolderStrategy implements SecurityContextHolderStrategy {
    
    private final ThreadLocal<SecurityContext> contextHolder = new ThreadLocal<>();

    @Override
    public void clearContext() {
        System.out.println("CustomSecurityContextHolderStrategy.clearContext()");
        contextHolder.remove();
    }
    
    @Override
    @NonNull
    public SecurityContext getContext() {
        System.out.println("CustomSecurityContextHolderStrategy.getContext()");
        SecurityContext context = getFromVaadinSession().orElseGet(contextHolder::get);
        if (context == null) {
            context = createEmptyContext();
            contextHolder.set(context);
        }
        
        return context;
    }

    @Override
    public void setContext(SecurityContext context) {
        System.out.println("CustomSecurityContextHolderStrategy.setContext()");
        VaadinSession vaadinSession = VaadinSession.getCurrent();
        if (vaadinSession != null) {
            vaadinSession.setAttribute(SecurityContext.class, context);
        }
    }

    @Override
    public SecurityContext createEmptyContext() {
        System.out.println("CustomSecurityContextHolderStrategy.createEmptyContext()");
        return new SecurityContextImpl();
    }
    
    @NonNull
    private Optional<SecurityContext> getFromVaadinSession() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null || session.getSession() == null) {
            return Optional.empty();
        }
        
        try {
            Object securityContext = session.getSession().getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            
            if (securityContext instanceof SecurityContext) {
                return Optional.of((SecurityContext) securityContext);
            } else {
                return Optional.empty();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            
            if (StringUtils.contains(e.getMessage(), "Session already invalidated")) {
                ((StandardSession)session.getSession()).expire();
            }
            
            throw e;
        }
    }
}