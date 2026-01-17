package saas.hotel.istoepousada.dto;

import java.util.List;

public record CnpjaResponse(
    String updated,
    String taxId,
    String alias,
    String founded,
    Boolean head,
    Company company,
    String statusDate,
    Status status,
    Address address,
    Activity mainActivity,
    List<Phone> phones,
    List<Email> emails,
    List<Activity> sideActivities,
    List<Object> suframa) {
  public record Company(
      List<Member> members,
      Long id,
      String name,
      Double equity,
      Nature nature,
      Size size,
      Regime simples,
      Regime simei) {}

  public record Member(String since, Person person, Role role) {}

  public record Person(String id, String type, String name, String taxId, String age) {}

  public record Role(Integer id, String text) {}

  public record Nature(Integer id, String text) {}

  public record Size(Integer id, String acronym, String text) {}

  public record Regime(Boolean optant, String since) {}

  public record Status(Integer id, String text) {}

  public record Address(
      Integer municipality,
      String street,
      String number,
      String district,
      String city,
      String state,
      String details,
      String zip,
      Country country) {}

  public record Country(Integer id, String name) {}

  public record Activity(Integer id, String text) {}

  public record Phone(String type, String area, String number) {}

  public record Email(String ownership, String address, String domain) {}
}
