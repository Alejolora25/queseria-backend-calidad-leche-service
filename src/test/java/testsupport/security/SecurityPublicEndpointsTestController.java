package testsupport.security;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecurityPublicEndpointsTestController {

  @PostMapping("/api/v1/auth/login")
  Map<String, String> login() {
    return Map.of("status", "ok");
  }

  @GetMapping("/actuator/health")
  Map<String, String> health() {
    return Map.of("status", "UP");
  }

  @GetMapping("/api/v1/proveedores/prueba")
  Map<String, String> proveedor() {
    return Map.of("status", "ok");
  }
}
