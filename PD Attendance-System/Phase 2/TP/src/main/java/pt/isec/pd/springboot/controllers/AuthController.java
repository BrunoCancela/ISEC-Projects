package pt.isec.pd.springboot.controllers;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.isec.pd.springboot.security.TokenService;

@RestController
public class AuthController
{
    private final TokenService tokenService;

    public AuthController(TokenService tokenService)
    {
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public String login(Authentication authentication)
    {
        return tokenService.generateToken(authentication);
    }

    @GetMapping("/authorization")
    public String authorization(Authentication authentication)
    {
        return authentication.getAuthorities().toString();
    }
}
