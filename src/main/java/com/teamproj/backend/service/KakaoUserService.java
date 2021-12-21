package com.teamproj.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamproj.backend.Repository.UserRepository;
import com.teamproj.backend.dto.kakao.HeaderDto;
import com.teamproj.backend.dto.kakao.KakaoUserInfoDto;
import com.teamproj.backend.model.User;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
@AllArgsConstructor
public class KakaoUserService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public HeaderDto kakaoLogin(
            String code
    ) throws JsonProcessingException {
        System.out.println(code);
// 1. "인가 코드"로 "액세스 토큰" 요청
        String accessToken = getAccessToken(code);

// 2. "액세스 토큰"으로 "카카오 사용자 정보" 가져오기
        KakaoUserInfoDto snsUserInfoDto = getKakaoUserInfo(accessToken);

// 3. "카카오 사용자 정보"로 필요시 회원가입  및 이미 같은 이메일이 있으면 기존회원으로 로그인
        User kakaoUser = registerKakaoOrUpdateKakao(snsUserInfoDto);

// 4. 강제 로그인 처리
//        HeaderDto token = forceLogin(kakaoUser);

//        return token;
        return null;
    }

    private String getAccessToken(
            String code
    ) throws JsonProcessingException {
// HTTP Header 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

// HTTP Body 생성
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", "316b336d315dff9b64eaa117a37ee25b");
        body.add("redirect_uri", "http://localhost:8080/oauth/callback/kakao");
//        body.add("redirect_uri", "http://localhost:3000/oauth/callback/kakao");
        // https://kauth.kakao.com/oauth/authorize?client_id=dcd2dc8ef9a91776b876f76145451b0f&redirect_uri=http://52.78.31.61:3000/oauth/kakao/callback&response_type=code
        body.add("code", code);



// HTTP 요청 보내기
        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest =
                new HttpEntity<>(body, headers);
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(
                "https://kauth.kakao.com/oauth/token",
                HttpMethod.POST,
                kakaoTokenRequest,
                String.class
        );

// HTTP 응답 (JSON) -> 액세스 토큰 파싱
        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        return jsonNode.get("access_token").asText();
    }

    private KakaoUserInfoDto getKakaoUserInfo(String accessToken) throws JsonProcessingException {
// HTTP Header 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

// HTTP 요청 보내기
        HttpEntity<MultiValueMap<String, String>> kakaoUserInfoRequest = new HttpEntity<>(headers);
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.POST,
                kakaoUserInfoRequest,
                String.class
        );

        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        Long id = jsonNode.get("id").asLong();
        String nickname = jsonNode.get("properties")
                .get("nickname").asText();
        String email;
        if(jsonNode.get("kakao_account").get("email") == null)
            email = UUID.randomUUID().toString() + "@contap.com";
        else
            email = jsonNode.get("kakao_account")
                    .get("email").asText();


        return new KakaoUserInfoDto(id, nickname, email);
    }

    private User registerKakaoOrUpdateKakao(
            KakaoUserInfoDto kakaoUserInfoDto
    ) {
        User sameUser = userRepository.findByKakaoId(kakaoUserInfoDto.getId())
                .orElse(null);

        if (sameUser == null) {
            return registerKakaoUserIfNeeded(kakaoUserInfoDto);
        } else {
//            return updateKakaoUser(sameUser, snsUserInfoDto);
            return null;
        }
    }

    private User registerKakaoUserIfNeeded(
            KakaoUserInfoDto kakaoUserInfoDto
    ) {
// DB 에 중복된 Kakao Id 가 있는지 확인
        Long kakaoId = kakaoUserInfoDto.getId();
        User kakaoUser = userRepository.findByKakaoId(kakaoId)
                .orElse(null);
        if (kakaoUser == null) {
            // 회원가입
            // username: kakao nickname
            String nickname = kakaoUserInfoDto.getNickname();

            // password: random UUID
            String password = UUID.randomUUID().toString();
            String encodedPassword = passwordEncoder.encode(password);

            // email: kakao email
            String email = kakaoUserInfoDto.getEmail();
            kakaoUser = User.builder()
//                    .email(email)
                    .username("test")
                    .password(encodedPassword)
                    .nickname(nickname)
                    .kakaoId(kakaoId)
                    .build();
            userRepository.save(kakaoUser);

        }
        return kakaoUser;
    }

//    private User updateKakaoUser(
//            User sameUser,
//            KakaoUserInfoDto snsUserInfoDto
//    ) {
//        if (sameUser.getKakaoId() == null) {
//            System.out.println("중복");
//            sameUser.setKakaoId(snsUserInfoDto.getId());
//            sameUser.setEmail(snsUserInfoDto.getEmail());
//            sameUser.setNickname(snsUserInfoDto.getNickname());
//            userRepository.save(sameUser);
//        }
//        return sameUser;
//    }

//    private HeaderDto forceLogin(
//            User kakaoUser
//    ) {
//        UserDetails userDetails = new UserDetailsImpl(kakaoUser);
//        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
//        SecurityContextHolder.getContext().setAuthentication(authentication);
//
//        HeaderDto headerDto = new HeaderDto();
//        headerDto.setTOKEN(JWTAuthProvider.createToken(kakaoUser.getNickname(),Long.toString(kakaoUser.getKakaoId())));
//        return headerDto;
//    }

}
