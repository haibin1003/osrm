package com.osrm.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osrm.application.h5.service.H5AccessTokenAppService;
import com.osrm.common.exception.BizException;
import com.osrm.common.model.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class H5TokenInterceptor implements HandlerInterceptor {

    @Autowired
    private H5AccessTokenAppService h5AccessTokenAppService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("X-H5-Token");
        try {
            h5AccessTokenAppService.validate(token);
            return true;
        } catch (BizException e) {
            response.setStatus(403);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            ApiResponse<Void> body = ApiResponse.error(403, e.getMessage());
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return false;
        }
    }
}
