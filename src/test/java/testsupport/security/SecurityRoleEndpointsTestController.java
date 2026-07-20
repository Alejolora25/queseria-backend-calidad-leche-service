package testsupport.security;

import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecurityRoleEndpointsTestController {
  @GetMapping({
      "/api/v1/proveedores/prueba",
      "/api/v1/muestras/prueba",
      "/api/v1/analiticas/prueba",
      "/api/v1/usuarios"
  })
  Map<String, String> consultar() {
    return ok();
  }

  @PostMapping({
      "/api/v1/proveedores",
      "/api/v1/muestras",
      "/api/v1/usuarios"
  })
  Map<String, String> crear() {
    return ok();
  }

  @PutMapping({
      "/api/v1/proveedores/1",
      "/api/v1/usuarios/2/roles"
  })
  Map<String, String> actualizar() {
    return ok();
  }

  @PatchMapping({
      "/api/v1/proveedores/1/activar",
      "/api/v1/usuarios/2/activar"
  })
  Map<String, String> cambiarEstado() {
    return ok();
  }

  @DeleteMapping("/api/v1/proveedores/1")
  Map<String, String> metodoNoContemplado() {
    return ok();
  }

  private Map<String, String> ok() {
    return Map.of("status", "ok");
  }
}
