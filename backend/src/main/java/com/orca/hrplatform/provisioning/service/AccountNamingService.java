package com.orca.hrplatform.provisioning.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.Locale;

@Service
public class AccountNamingService {

    public String generateLogin(String fullName) {
        String[] parts = normalize(fullName).split("\\s+");
        if (parts.length < 2) {
            return normalize(fullName).replace(" ", ".");
        }

        String firstInitial = parts[1].substring(0, 1);
        String lastName = parts[0];
        return (firstInitial + "." + lastName).toLowerCase(Locale.ROOT);
    }

    public String generateEmail(String login, String domain) {
        return login + "@" + domain;
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        String transliterated = transliterateCyrillic(value.trim());
        String withoutAccents = Normalizer.normalize(transliterated, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return withoutAccents
                .replaceAll("[^A-Za-z0-9\\s-]", "")
                .replaceAll("[-_]+", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String transliterateCyrillic(String value) {
        String[][] mappings = {
                {"А", "A"}, {"Ә", "A"}, {"Б", "B"}, {"В", "V"}, {"Г", "G"}, {"Ғ", "G"}, {"Д", "D"}, {"Е", "E"}, {"Ё", "E"}, {"Ж", "Zh"},
                {"З", "Z"}, {"И", "I"}, {"Й", "Y"}, {"К", "K"}, {"Қ", "K"}, {"Л", "L"}, {"М", "M"}, {"Н", "N"}, {"Ң", "N"}, {"О", "O"},
                {"Ө", "O"}, {"П", "P"}, {"Р", "R"}, {"С", "S"}, {"Т", "T"}, {"У", "U"}, {"Ұ", "U"}, {"Ү", "U"}, {"Ф", "F"}, {"Х", "Kh"},
                {"Һ", "H"}, {"Ц", "Ts"}, {"Ч", "Ch"}, {"Ш", "Sh"}, {"Щ", "Shch"}, {"Ъ", ""}, {"Ы", "Y"}, {"І", "I"}, {"Ь", ""},
                {"Э", "E"}, {"Ю", "Yu"}, {"Я", "Ya"}
        };

        String result = value;
        for (String[] mapping : mappings) {
            result = result.replace(mapping[0], mapping[1]);
            result = result.replace(mapping[0].toLowerCase(Locale.ROOT), mapping[1].toLowerCase(Locale.ROOT));
        }
        return result;
    }
}
