package dev.incident;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name="investigations")
public class Investigation {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @Column(nullable=false) public Long incidentId;
  @Column(columnDefinition="text") public String resultJson;
  public Instant createdAt = Instant.now();
  public Investigation() {}
  public Investigation(Long incidentId,String resultJson) { this.incidentId=incidentId; this.resultJson=resultJson; }
}