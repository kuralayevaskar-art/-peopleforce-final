package com.orca.hrplatform.integration.ad.service;

import com.orca.hrplatform.integration.ad.config.AdProperties;
import com.orca.hrplatform.integration.ad.dto.AdConnectionTestResponse;
import com.orca.hrplatform.integration.ad.dto.AdSettingsRequest;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.naming.directory.SearchControls;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdDirectoryService {

    private static final List<String> STAFF_SEARCH_BASES = List.of(
            "OU=Staff,OU=DMUK,DC=DMUK,DC=EDU",
            "OU=AcademicStaff,OU=DMUK,DC=DMUK,DC=EDU"
    );
    private static final List<String> BLOCKED_OU_MARKERS = List.of(
            "ou=students,",
            "ou=f2025,",
            "ou=lab accounts,",
            "ou=labaccounts,",
            "ou=test,"
    );

    private final AdProperties properties;

    public AdUser authenticate(String username, String password) {
        if (!properties.isEnabled()) {
            throw new BadCredentialsException("AD integration is disabled");
        }
        if (!StringUtils.hasText(password)) {
            throw new BadCredentialsException("Password is required");
        }

        AdUser user = findUser(username, properties);
        try {
            buildTemplate(properties.getUrl(), user.getDn(), password).getContextSource().getContext(user.getDn(), password).close();
        } catch (Exception ex) {
            throw new BadCredentialsException("Invalid AD username or password");
        }

        return user;
    }

    public AdConnectionTestResponse test(AdSettingsRequest request) {
        AdProperties effective = copyFromRequest(request);
        buildTemplate(effective.getUrl(), effective.getBindDn(), effective.getBindPassword()).getContextSource().getContext(effective.getBindDn(), effective.getBindPassword());

        return AdConnectionTestResponse.builder()
                .connected(true)
                .url(effective.getUrl())
                .baseDn(effective.getBaseDn())
                .message("AD/LDAP connection successful")
                .build();
    }

    public List<AdUser> listUsers(AdSettingsRequest request) {
        AdProperties effective = copyFromRequest(request);
        LdapTemplate template = buildTemplate(effective.getUrl(), effective.getBindDn(), effective.getBindPassword());
        List<AdUser> users = new ArrayList<>();
        ContextMapper<AdUser> mapper = context -> {
            DirContextAdapter adapter = (DirContextAdapter) context;
            String username = adapter.getStringAttribute("sAMAccountName");
            return AdUser.builder()
                    .dn(adapter.getNameInNamespace())
                    .username(username != null ? username : adapter.getStringAttribute("cn"))
                    .email(adapter.getStringAttribute("mail"))
                    .displayName(adapter.getStringAttribute("displayName") != null ? adapter.getStringAttribute("displayName") : adapter.getStringAttribute("cn"))
                    .department(firstText(adapter.getStringAttribute("department"), departmentFromDn(adapter.getNameInNamespace())))
                    .title(firstText(adapter.getStringAttribute("title"), adapter.getStringAttribute("description"), "Сотрудник"))
                    .manager(adapter.getStringAttribute("manager"))
                    .build();
        };

        for (String searchBase : searchBases(effective)) {
            users.addAll(template.search(searchBase, "(&(objectClass=user)(!(objectClass=computer)))", SearchControls.SUBTREE_SCOPE, mapper));
        }

        Map<String, AdUser> uniqueUsers = new LinkedHashMap<>();
        for (AdUser user : users) {
            String key = firstText(user.getUsername(), user.getEmail(), user.getDn()).toLowerCase(Locale.ROOT);
            uniqueUsers.putIfAbsent(key, user);
        }

        return uniqueUsers.values().stream()
                .filter(this::isAllowedStaffUser)
                .sorted(Comparator
                        .comparing((AdUser user) -> nullSafe(user.getDepartment()), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(user -> nullSafe(user.getDisplayName()), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private AdUser findUser(String username, AdProperties effective) {
        LdapTemplate template = buildTemplate(effective.getUrl(), effective.getBindDn(), effective.getBindPassword());
        String filter = effective.getUserFilter().replace("{0}", username).replace("{username}", username);
        List<String> searchBases = searchBases(effective);

        ContextMapper<AdUser> mapper = context -> {
            DirContextAdapter adapter = (DirContextAdapter) context;
            return AdUser.builder()
                    .dn(adapter.getNameInNamespace())
                    .username(username)
                    .email(adapter.getStringAttribute("mail") != null ? adapter.getStringAttribute("mail") : username + "@ad.local")
                    .displayName(adapter.getStringAttribute("displayName") != null ? adapter.getStringAttribute("displayName") : username)
                    .department(firstText(adapter.getStringAttribute("department"), departmentFromDn(adapter.getNameInNamespace())))
                    .title(firstText(adapter.getStringAttribute("title"), adapter.getStringAttribute("description"), "Сотрудник"))
                    .manager(adapter.getStringAttribute("manager"))
                    .build();
        };

        List<AdUser> users = new ArrayList<>();
        for (String searchBase : searchBases) {
            users.addAll(template.search(searchBase, filter, SearchControls.SUBTREE_SCOPE, mapper));
            if (!users.isEmpty()) {
                break;
            }
        }

        if (users.isEmpty()) {
            throw new BadCredentialsException("AD user not found");
        }

        return users.getFirst();
    }

    private LdapTemplate buildTemplate(String url, String userDn, String password) {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(url);
        contextSource.setUserDn(userDn);
        contextSource.setPassword(password);
        contextSource.afterPropertiesSet();

        return new LdapTemplate(contextSource);
    }

    private AdProperties copyFromRequest(AdSettingsRequest request) {
        AdProperties effective = new AdProperties();
        effective.setEnabled(true);
        effective.setUrl(StringUtils.hasText(request.getUrl()) ? request.getUrl() : properties.getUrl());
        effective.setBaseDn(StringUtils.hasText(request.getBaseDn()) ? request.getBaseDn() : properties.getBaseDn());
        effective.setBindDn(StringUtils.hasText(request.getBindDn()) ? request.getBindDn() : properties.getBindDn());
        effective.setBindPassword(StringUtils.hasText(request.getBindPassword()) ? request.getBindPassword() : properties.getBindPassword());
        effective.setUserFilter(StringUtils.hasText(request.getUserFilter()) ? request.getUserFilter() : properties.getUserFilter());
        effective.setGroupFilter(StringUtils.hasText(request.getGroupFilter()) ? request.getGroupFilter() : properties.getGroupFilter());
        effective.setDefaultRoleCode(properties.getDefaultRoleCode());
        effective.setSearchBaseDns(request.getSearchBaseDns() != null && !request.getSearchBaseDns().isEmpty()
                ? request.getSearchBaseDns()
                : properties.getSearchBaseDns());
        return effective;
    }

    private List<String> searchBases(AdProperties effective) {
        if (effective.getSearchBaseDns() != null && !effective.getSearchBaseDns().isEmpty()) {
            List<String> allowedBases = effective.getSearchBaseDns().stream()
                    .filter(StringUtils::hasText)
                    .filter(this::isStaffSearchBase)
                    .toList();
            if (!allowedBases.isEmpty()) {
                return allowedBases;
            }
        }

        return STAFF_SEARCH_BASES;
    }

    private boolean isStaffSearchBase(String searchBase) {
        String normalized = normalizeDn(searchBase);
        return STAFF_SEARCH_BASES.stream()
                .map(this::normalizeDn)
                .anyMatch(normalized::equals);
    }

    private boolean isAllowedStaffUser(AdUser user) {
        String dn = normalizeDn(user.getDn());
        boolean inAllowedOu = STAFF_SEARCH_BASES.stream()
                .map(this::normalizeDn)
                .anyMatch(dn::contains);
        boolean inBlockedOu = BLOCKED_OU_MARKERS.stream().anyMatch(dn::contains);
        return inAllowedOu && !inBlockedOu && StringUtils.hasText(user.getUsername());
    }

    private String departmentFromDn(String dn) {
        if (!StringUtils.hasText(dn)) {
            return "";
        }

        for (String part : dn.split(",")) {
            String trimmed = part.trim();
            if (trimmed.regionMatches(true, 0, "OU=", 0, 3)) {
                String ou = trimmed.substring(3);
                String normalized = ou.replace(" ", "").toLowerCase(Locale.ROOT);
                if (!List.of("staff", "academicstaff", "students", "f2025", "test", "labaccounts", "dmuk").contains(normalized)) {
                    return ou;
                }
            }
        }

        return "";
    }

    private String normalizeDn(String value) {
        return nullSafe(value).replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }

        return "";
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    @Data
    @Builder
    public static class AdUser {
        private String dn;
        private String username;
        private String email;
        private String displayName;
        private String department;
        private String title;
        private String manager;
    }
}
