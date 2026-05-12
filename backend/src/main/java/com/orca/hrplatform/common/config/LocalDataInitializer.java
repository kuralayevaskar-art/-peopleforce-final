package com.orca.hrplatform.common.config;

import com.orca.hrplatform.auth.entity.Role;
import com.orca.hrplatform.auth.entity.User;
import com.orca.hrplatform.auth.entity.UserStatus;
import com.orca.hrplatform.auth.repository.RoleRepository;
import com.orca.hrplatform.auth.repository.UserRepository;
import com.orca.hrplatform.company.entity.Company;
import com.orca.hrplatform.company.entity.CompanyStatus;
import com.orca.hrplatform.company.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDataInitializer implements CommandLineRunner {

    private final CompanyRepository companyRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        Company company = companyRepository.save(Company.builder()
                .name("Orca Demo Company")
                .bin("000000000000")
                .email("hr@demo.com")
                .status(CompanyStatus.ACTIVE)
                .build());

        Role hrAdmin = roleRepository.save(Role.builder()
                .code("HR_ADMIN")
                .name("HR Administrator")
                .build());

        User user = User.builder()
                .companyId(company.getId())
                .email("hr@demo.com")
                .passwordHash(passwordEncoder.encode("Admin123!"))
                .status(UserStatus.ACTIVE)
                .build();
        user.getRoles().add(hrAdmin);

        userRepository.save(user);
    }
}
