package com.example.rest.error;

import com.example.document.exception.ResourceAlreadyExistsException;
import com.example.document.exception.ResourceNotFoundException;
import com.example.document.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RestExceptionHandlerTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();
    private final WebRequest webRequest = new ServletWebRequest(new MockHttpServletRequest());

    @Test
    void testHandleIo() {
        RuntimeException ex = new RuntimeException("IO Error");
        ResponseEntity<Object> response = handler.handleIo(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isInstanceOf(RestError.class);
        RestError error = (RestError) response.getBody();
        assertThat(error.body()).containsExactly("Connection Error with the Database");
    }

    @Test
    void testHandleConflict() {
        ResourceAlreadyExistsException ex = new ResourceAlreadyExistsException("Already exists");
        ResponseEntity<Object> response = handler.handleConflict(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isInstanceOf(RestError.class);
        RestError error = (RestError) response.getBody();
        assertThat(error.body()).containsExactly("Already exists");
    }

    @Test
    void testHandleNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Not found");
        ResponseEntity<Object> response = handler.handleNotFound(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isInstanceOf(RestError.class);
        RestError error = (RestError) response.getBody();
        assertThat(error.body()).containsExactly("Not found");
    }

    @Test
    void testHandleUnauthorized() {
        UnauthorizedException ex = new UnauthorizedException("Unauthorized");
        ResponseEntity<Object> response = handler.handleUnauthorized(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isInstanceOf(RestError.class);
        RestError error = (RestError) response.getBody();
        assertThat(error.body()).containsExactly("Unauthorized");
    }

    @Test
    void testHandleUnexpected() {
        RuntimeException ex = new RuntimeException("Unexpected error");
        ResponseEntity<Object> response = handler.handleUnexpected(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isInstanceOf(RestError.class);
        RestError error = (RestError) response.getBody();
        assertThat(error.body()).containsExactly("Unexpected error");
    }

    @Test
    void testRestErrorRecord() {
        RestError error = new RestError(List.of("error1", "error2"));
        assertThat(error.body()).containsExactly("error1", "error2");
    }
}
