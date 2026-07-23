package com.querylens.backend.dto.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ApiErrorResponseTest {

    @Test
    void nullErrorsBecomeAnEmptyImmutableList() {
        var response = new ApiErrorResponse("Unable to parse SQL schema", null);

        assertThat(response.errors()).isEmpty();
        assertThatThrownBy(() -> response.errors().add("error"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void errorsAreDefensivelyCopiedAndPreserveOrder() {
        var errors = new ArrayList<>(List.of("First error", "Second error"));

        var response = new ApiErrorResponse("Unable to parse SQL schema", errors);
        errors.clear();

        assertThat(response.errors()).containsExactly("First error", "Second error");
        assertThatThrownBy(() -> response.errors().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
