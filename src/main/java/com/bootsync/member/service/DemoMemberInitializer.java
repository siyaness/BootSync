package com.bootsync.member.service;

import com.bootsync.member.entity.Member;
import com.bootsync.member.repository.MemberRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile({"local", "test"})
public class DemoMemberInitializer {

    @Bean
    @Order(10)
    ApplicationRunner demoMemberApplicationRunner(
        MemberRepository memberRepository,
        PasswordEncoder passwordEncoder,
        Clock clock,
        @Value("${spring.security.user.name:d}") String demoUsername,
        @Value("${spring.security.user.password:d}") String demoPassword
    ) {
        return args -> {
            if (memberRepository.findByUsername(demoUsername).isPresent()) {
                return;
            }

            LocalDateTime now = LocalDateTime.now(clock);

            Member member = new Member();
            member.setUsername(demoUsername);
            member.setPasswordHash(passwordEncoder.encode(demoPassword));
            member.setDisplayName(demoUsername);
            member.setCreatedAt(now);
            member.setUpdatedAt(now);

            memberRepository.save(member);
        };
    }
}
