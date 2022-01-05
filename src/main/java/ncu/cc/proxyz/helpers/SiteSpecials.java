package ncu.cc.proxyz.helpers;

import java.security.Principal;
import java.util.Map;

public class SiteSpecials {
    private static final String PERSONAL_NO_ENCODE = "personalNoEncode";
    private static final String ACCOUNT_TYPE = "accountType";

    public static String attributeToName(Principal principal, Map<String, Object> attributes) {
        return attributes.keySet().stream()
                .filter(SiteSpecials.ACCOUNT_TYPE::equalsIgnoreCase)
                .map(attributes::get)
                .findFirst()
                .map(Object::toString)
                .filter("SPARC"::equalsIgnoreCase)
                .map(s -> principal.getName())
                .orElseGet(() -> attributes.keySet().stream()
                        .filter(SiteSpecials.PERSONAL_NO_ENCODE::equalsIgnoreCase)
                        .map(attributes::get)
                        .findFirst()
                        .map(Object::toString)
                        .orElse(""));
    }
}
