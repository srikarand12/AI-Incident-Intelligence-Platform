package dev.incident;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
@Entity
@Table(name="incidents")
public class Incident {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @NotBlank public String title;
  @Column(columnDefinition="text") public String description;
  @NotBlank public String serviceName;
  public String severity = "MEDIUM";
  public String status = "OPEN";
  public Instant createdAt = Instant.now();
  public Incident() {}
}