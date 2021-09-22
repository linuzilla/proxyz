package ncu.cc.proxyz.helpers;


import ncu.cc.proxyz.constants.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class CookieHelper {
    private static final Logger logger = LoggerFactory.getLogger(CookieHelper.class);
    private static final List<String> PROTECTED_LOCAL_COOKIE = List.of(
            Constants.SHH_COOKIE_NAME
    );

    public static String filterCookie(List<List<String>> cookieStrings, Predicate<String[]> predicate) {
        return ListHelper.listOfListFlattening(cookieStrings)
                .map(s -> List.of(s.split("; ")))
                .flatMap(Collection::stream)
                .map(s -> s.split("="))
                .filter(strings -> strings.length == 2)
                .filter(predicate)
                .map(strings -> strings[0] + "=" + strings[1])
                .collect(Collectors.joining("; "));
    }

    public static boolean cookieNameFilter(String cookieName) {
        return PROTECTED_LOCAL_COOKIE.stream()
                .noneMatch(s -> s.equals(cookieName));
    }
}
