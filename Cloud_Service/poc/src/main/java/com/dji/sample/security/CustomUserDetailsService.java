package com.dji.sample.security;

import com.dji.sample.entity.User;
import com.dji.sample.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail)
            throws UsernameNotFoundException {

        User user = userRepository
                .findByUsernameAndDeletedAtIsNull(usernameOrEmail)
                .or(() -> userRepository.findByEmailAndDeletedAtIsNull(usernameOrEmail))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (user.getDeletedAt() != null || !Boolean.TRUE.equals(user.getIsActive())) {
            throw new UsernameNotFoundException("User account is inactive");
        }

        return new CustomUserDetails(user);
    }
}