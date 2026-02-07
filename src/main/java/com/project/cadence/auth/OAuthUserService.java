package com.project.cadence.auth;

import com.project.cadence.events.UserCreatedEvent;
import com.project.cadence.model.OAuth2Provider;
import com.project.cadence.model.Role;
import com.project.cadence.model.User;
import com.project.cadence.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OAuthUserService {
    private final UserRepository userRepository;
    private final ApplicationEventPublisher publisher;

    @Transactional
    public User findOrCreateUser(String email, String name, String picture) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setName(name);
                    newUser.setProfileUrl(picture);
                    newUser.setProvider(OAuth2Provider.GOOGLE);
                    newUser.setRole(Role.USER);

                    User saved = userRepository.save(newUser);

                    publisher.publishEvent(
                            new UserCreatedEvent(saved.getId())
                    );

                    return saved;
                });
    }
}
