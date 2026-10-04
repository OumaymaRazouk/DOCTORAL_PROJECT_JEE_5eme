package com.exemple.utilisateursservice.services;

import com.exemple.utilisateursservice.dto.*;
import com.exemple.utilisateursservice.entities.RefreshToken;
import com.exemple.utilisateursservice.entities.User;
import com.exemple.utilisateursservice.model.Role;
import com.exemple.utilisateursservice.repositories.RefreshTokenRepository;
import com.exemple.utilisateursservice.repositories.UserRepository;
import com.exemple.utilisateursservice.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.Year;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository, PasswordEncoder encoder, AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.encoder = encoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) throw new IllegalArgumentException("Email already used");
        Set<Role> roles = (req.getRoles() == null || req.getRoles().isEmpty()) ? Set.of(Role.CANDIDAT) : req.getRoles();
        var user = User.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(req.getEmail())
                .password(encoder.encode(req.getPassword()))
                .roles(roles)
                .enabled(true)
                .build();
        userRepository.save(user);
        return issueTokens(user);
    }

    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        var user = userRepository.findByEmail(req.getEmail()).orElseThrow();
        return issueTokens(user);
    }

    public AuthResponse refresh(RefreshRequest req) {
        var rt = refreshTokenRepository.findByToken(req.getRefreshToken()).orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        if (rt.isRevoked() || rt.getExpiresAt().isBefore(Instant.now())) throw new IllegalArgumentException("Expired/Revoked refresh token");
        var user = rt.getUser();
        return issueTokens(user);
    }

    public void logout(String email) {
        var user = userRepository.findByEmail(email).orElse(null);
        if (user!=null) refreshTokenRepository.deleteByUser_Id(user.getId());
    }

    public Map<String, Object> createDirector(CreateDirectorRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) throw new IllegalArgumentException("Email already used");
        var token = UUID.randomUUID().toString();
        var expires = Instant.now().plusSeconds(48 * 3600);
        var director = User.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(req.getEmail())
                .password(encoder.encode(UUID.randomUUID().toString()))
                .enabled(false)
                .etablissement(req.getEtablissement())
                .specialite(req.getSpecialite())
                .activationToken(token)
                .activationExpiresAt(expires)
                .roles(Set.of(Role.DIRECTEUR))
                .build();
        director = userRepository.save(director);
        var identifiant = String.format("DIR-%d-%06d", Year.now().getValue(), director.getId());
        director.setIdentifiant(identifiant);
        userRepository.save(director);
        Map<String, Object> res = new HashMap<>();
        res.put("id", director.getId());
        res.put("identifiant", identifiant);
        res.put("email", director.getEmail());
        res.put("activationToken", token);
        res.put("activationExpiresAt", expires.toString());
        return res;
    }

    public AuthResponse activate(ActivateRequest req) {
        var u = userRepository.findByActivationToken(req.getToken()).orElseThrow(() -> new IllegalArgumentException("Invalid token"));
        if (u.getActivationExpiresAt() == null || u.getActivationExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Expired token");
        }
        u.setPassword(encoder.encode(req.getNewPassword()));
        u.setEnabled(true);
        u.setActivationToken(null);
        u.setActivationExpiresAt(null);
        userRepository.save(u);
        return issueTokens(u);
    }

    private AuthResponse issueTokens(User user) {
        Map<String,Object> claims = new HashMap<>();
        claims.put("roles", user.getRoles());
        String access = jwtService.generateAccessToken(user.getEmail(), claims);
        String refresh = jwtService.generateRefreshToken(user.getEmail());
        var rt = RefreshToken.builder().token(refresh).user(user).expiresAt(Instant.now().plusSeconds(7*24*3600)).revoked(false).build();
        refreshTokenRepository.save(rt);
        return AuthResponse.builder().accessToken(access).refreshToken(refresh).tokenType("Bearer").build();
    }
}
