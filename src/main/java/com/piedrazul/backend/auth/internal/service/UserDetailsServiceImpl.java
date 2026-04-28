package com.piedrazul.backend.auth.internal.service;

import com.piedrazul.backend.auth.internal.domain.Usuario;
import com.piedrazul.backend.auth.internal.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementación de UserDetailsService para Spring Security.
 * Carga el usuario desde la BD por username para validar credenciales.
 */
@Service
@Deprecated(since = "keycloak-migration", forRemoval = false)
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        String rol = usuario.getRol() == null ? "" : usuario.getRol();

        return new org.springframework.security.core.userdetails.User(
                usuario.getUsername(),
                "",
                List.of(new SimpleGrantedAuthority("ROLE_" + rol))
        );
    }
}