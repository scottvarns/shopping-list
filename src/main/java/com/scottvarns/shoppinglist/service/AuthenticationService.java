package com.scottvarns.shoppinglist.service;

import com.scottvarns.shoppinglist.dto.request.LoginRequestDTO;
import com.scottvarns.shoppinglist.dto.request.SignupRequestDTO;
import com.scottvarns.shoppinglist.dto.response.AuthenticationResponseDTO;
import com.scottvarns.shoppinglist.dto.response.UserResponseDTO;

public interface AuthenticationService {

    UserResponseDTO signup(SignupRequestDTO request);

    AuthenticationResponseDTO login(LoginRequestDTO request);
}
