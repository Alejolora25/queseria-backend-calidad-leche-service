package com.queseria.calidadleche.infrastructure.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class HashUtil {

  private static final ObjectMapper MAPPER = new ObjectMapper()
      .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
      .setSerializationInclusion(JsonInclude.Include.NON_NULL);

  private HashUtil(){}

  public static String sha256CanonicalJson(Object obj) {
    try {
      String json = MAPPER.writeValueAsString(obj);
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
      return toHex(hash);
    } catch (JsonProcessingException | NoSuchAlgorithmException e) {
      throw new IllegalStateException("Error calculando hash", e);
    }
  }

  private static String toHex(byte[] bytes){
    StringBuilder sb = new StringBuilder(bytes.length*2);
    for (byte b: bytes) sb.append(String.format("%02x", b));
    return "sha256:" + sb;
  }
}
