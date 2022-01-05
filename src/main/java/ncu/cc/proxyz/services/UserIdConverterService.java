package ncu.cc.proxyz.services;

import java.security.Principal;

public interface UserIdConverterService {
    String convert(Principal principal);
}
