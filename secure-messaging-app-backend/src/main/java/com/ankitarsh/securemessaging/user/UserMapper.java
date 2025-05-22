package com.ankitarsh.securemessaging.user;

import com.ankitarsh.securemessaging.authentication.LoginResponseDTO;
import com.ankitarsh.securemessaging.authentication.RegisterRequestDTO;
import org.springframework.stereotype.Service;

@Service
public class UserMapper {

    public User toEntityFromDTO(UserDTO userDTO){
        if (userDTO == null) {
            return null;
        }
        User user = new User();
        user.setId(userDTO.id());
        user.setUserName(userDTO.userName());
        user.setEmailId(userDTO.emailId());
        user.setMobileNumber(userDTO.mobileNumber());
        user.setPassword(userDTO.password());
        return user;
    }

    public UserResponseDTO toResponseDTO(User user){
        if (user == null) {
            return null;
        }
        return new UserResponseDTO(
                user.getId(),
                user.getUserName(),
                user.getEmailId(),
                user.getMobileNumber(),
                user.getStatus()
        );
    }

    public LoginResponseDTO toLoginResponseDTO(User user, String token){
        if (user == null) {
            return null;
        }
        return new LoginResponseDTO(
                user.getId(),
                user.getUserName(),
                user.getEmailId(),
                user.getMobileNumber(),
                user.getStatus(),
                token);
    }

    public User toEntityFromRegisterRequest(RegisterRequestDTO registerRequestDTO){
        if (registerRequestDTO == null) {
            return null;
        }
        User user = new User();
        user.setUserName(registerRequestDTO.userName());
        user.setEmailId(registerRequestDTO.emailId());
        user.setMobileNumber(registerRequestDTO.mobileNumber());
        user.setPassword(registerRequestDTO.password());
        return user;
    }
}
