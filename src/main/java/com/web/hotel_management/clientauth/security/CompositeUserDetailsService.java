package com.web.hotel_management.clientauth.security;

import com.web.hotel_management.clientauth.service.ClientAuthService;
import com.web.hotel_management.client.entity.Client;
import com.web.hotel_management.client.repository.ClientRepository;
import com.web.hotel_management.user.entity.User;
import com.web.hotel_management.user.entity.UserRole;
import com.web.hotel_management.user.repository.UserRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@Primary
public class CompositeUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;

    public CompositeUserDetailsService(UserRepository userRepository, ClientRepository clientRepository) {
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String subject) throws UsernameNotFoundException {
        if (subject != null && subject.startsWith(ClientAuthService.CLIENT_SUBJECT_PREFIX)) {
            String phone = subject.substring(ClientAuthService.CLIENT_SUBJECT_PREFIX.length()).trim();
            Client client = clientRepository.findByPhone(phone)
                    .orElseThrow(() -> new UsernameNotFoundException("Client not found with phone: " + phone));
            return new org.springframework.security.core.userdetails.User(
                    client.getPhone(),
                    client.getPasswordHash(),
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_CLIENT"))
            );
        }

        User user = userRepository.findByUsername(subject)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + subject));

        UserRole role = user.getRole() != null ? user.getRole() : UserRole.SYSTEM;
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
    }
}

