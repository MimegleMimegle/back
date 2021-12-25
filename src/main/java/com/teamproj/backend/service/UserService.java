package com.teamproj.backend.service;

import com.teamproj.backend.Repository.UserRepository;
import com.teamproj.backend.dto.user.signUp.SignUpCheckResponseDto;
import com.teamproj.backend.dto.user.signUp.SignUpRequestDto;
import com.teamproj.backend.dto.user.signUp.SignUpResponseDto;
import com.teamproj.backend.dto.user.userInfo.UserInfoResponseDto;
import com.teamproj.backend.dto.user.userInfo.UserNicknameModifyRequestDto;
import com.teamproj.backend.dto.user.userInfo.UserNicknameModifyResponseDto;
import com.teamproj.backend.model.User;
import com.teamproj.backend.security.UserDetailsImpl;
import com.teamproj.backend.util.JwtAuthenticateProcessor;
import com.teamproj.backend.util.ValidChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Optional;

import static com.teamproj.backend.exception.ExceptionMessages.*;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtAuthenticateProcessor jwtAuthenticateProcessor;

    // 회원가입 기능
    public SignUpResponseDto signUp(SignUpRequestDto signUpRequestDto) {
        // 아이디는 모두 소문자로 저장.
        signUpRequestDto.setUsername(signUpRequestDto.getUsername().toLowerCase());
        signUpValidCheck(signUpRequestDto);
        String encodedPassword = passwordEncoder.encode(signUpRequestDto.getPassword());

        userRepository.save(new User(signUpRequestDto, encodedPassword));

        return SignUpResponseDto.builder()
                .username(signUpRequestDto.getUsername())
                .nickname(signUpRequestDto.getNickname())
                .build();
    }

    public UserInfoResponseDto getUserInfo(UserDetailsImpl userDetails) {
        ValidChecker.loginCheck(userDetails);

        return UserInfoResponseDto.builder()
                .username(userDetails.getUsername())
                .nickname(jwtAuthenticateProcessor.getUser(userDetails).getNickname())
                .build();
    }

    public SignUpCheckResponseDto usernameValidCheck(String username) {
        Optional<User> found = userRepository.findByUsername(username);
        System.out.println("아이디 중복검사 결과 : " + !found.isPresent());
        return SignUpCheckResponseDto.builder()
                .result(!found.isPresent())
                .build();
    }

    public SignUpCheckResponseDto nicknameValidCheck(String nickname) {
        Optional<User> found = userRepository.findByNickname(nickname);
        System.out.println("닉네임 중복검사 결과 : " + !found.isPresent());
        return SignUpCheckResponseDto.builder()
                .result(!found.isPresent())
                .build();
    }

    @Transactional
    public UserNicknameModifyResponseDto nicknameModify(UserDetailsImpl userDetails,
                                                        UserNicknameModifyRequestDto userNicknameModifyRequestDto) {
        ValidChecker.loginCheck(userDetails);

        User user = jwtAuthenticateProcessor.getUser(userDetails);
        user.setNickname(userNicknameModifyRequestDto.getNickname());

        return UserNicknameModifyResponseDto.builder()
                .result("변경 완료")
                .build();
    }


    // region 보조 기능
    // Utils
    // 회원가입 유효성 검사
    private void signUpValidCheck(SignUpRequestDto signUpRequestDto) {
        String username = signUpRequestDto.getUsername();
        String nickname = signUpRequestDto.getNickname();
        String password = signUpRequestDto.getPassword();

        /*
            아이디 : 3자 이상, 16자 이하, 대소문자, 숫자
            닉네임 : 2자 이상, 16자 이하, 대소문자 숫자 한글
            비밀번호 : 6자 이상 16자 이하, 대소문자 숫자 조합
         */
        // username
        if (username.length() < 3 || username.length() > 16) {
            throw new IllegalArgumentException(ILLEGAL_USERNAME_LENGTH);
        } else if (!username.matches("^(?=.*[a-z0-9])[a-z0-9]{3,16}$")) {
            throw new IllegalArgumentException(ILLEGAL_USERNAME_FORMAT);
        }
        // nickname
        if (nickname.length() < 2 || nickname.length() > 16) {
            throw new IllegalArgumentException(ILLEGAL_NICKNAME_LENGTH);
        } else if (!nickname.matches("^(?=.*[a-z0-9가-힣])[a-z0-9가-힣]{2,16}$")) {
            throw new IllegalArgumentException(ILLEGAL_NICKNAME_FORMAT);
        }
        // password
        if (password.length() < 6 || password.length() > 16) {
            throw new IllegalArgumentException(ILLEGAL_PASSWORD_LENGTH);
        } else if (!password.matches("^(?=.*[0-9])(?=.*[a-zA-Z])[a-zA-Z0-9!@#$%^&*()._-]{6,16}$")) {
            throw new IllegalArgumentException(ILLEGAL_PASSWORD_FORMAT);
        }

        // 중복검사
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException(EXIST_USERNAME);
        }
        if (userRepository.existsByNickname(nickname)) {
            throw new IllegalArgumentException(EXIST_NICKNAME);
        }
        if (!signUpRequestDto.getPassword().equals(signUpRequestDto.getPasswordCheck())) {
            throw new IllegalArgumentException(ILLEGAL_MATCHING_PASSWORD_PASSWORD_CHECK);
        }
    }


    // endregion
}
