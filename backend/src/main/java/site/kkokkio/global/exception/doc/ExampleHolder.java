package site.kkokkio.global.exception.doc;

import io.swagger.v3.oas.models.examples.Example;
import lombok.Builder;

@Builder
public record ExampleHolder(Example example, int code, String name) {
}