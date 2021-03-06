package com.ovh.mls.serving.runtime.torch;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Header;
import com.ovh.mls.serving.runtime.IsCloseTo;
import com.ovh.mls.serving.runtime.core.ApiServer;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.swagger.v3.oas.integration.OpenApiContextLocator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class TorchSimpleIT {

    @BeforeAll
    public static void startServer() throws NoSuchFieldException, IllegalAccessException {
        // Reset swagger context
        Field field = OpenApiContextLocator.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, null);

        Config config = ConfigFactory.load("torch/simple_model/api.conf");
        ApiServer apiServer = new ApiServer(config);
        apiServer.start();
        RestAssured.port = 8093;
    }

    @Test
    public void testDescribe() {
        given()
            .when()
            .get("/describe")
            .then()
            .statusCode(200)
            .body(
                "inputs.get(0).name", equalTo("input_0"),
                "inputs.get(0).shape", equalTo(List.of(2)),
                "inputs.get(0).type", equalTo("float"),
                "outputs.get(0).name", equalTo("output_0"),
                "outputs.get(0).shape", equalTo(List.of(2)),
                "outputs.get(0).type", equalTo("float")
            );
    }

    @Test
    public void testEvalString() {
        Map<String, Object> body = new HashMap<>();
        body.put("input_0", List.of(0.5, 0.2));
        given()
            .body(body)
            .header(new Header("Content-Type", "application/json"))
            .when()
            .post("/eval")
            .then()
            .statusCode(200)
            .body(
                "output_0.get(0)", IsCloseTo.closeTo(0.120716706f, 1e-6f),
                "output_0.get(1)", IsCloseTo.closeTo(0.054772355f, 1e-6f)
            );
    }
}
