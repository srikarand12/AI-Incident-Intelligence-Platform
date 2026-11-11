package dev.incident;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface InvestigationRepository extends JpaRepository<Investigation,Long> {
  List<Investigation> findByIncidentIdOrderByCreatedAtDesc(Long incidentId);
}