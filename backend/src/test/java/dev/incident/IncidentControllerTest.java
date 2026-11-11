package dev.incident;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
class IncidentControllerTest {
 @Test void missingIncidentReturns404() {
   IncidentRepository repo=mock(IncidentRepository.class);
   InvestigationRepository history=mock(InvestigationRepository.class);
   when(repo.findById(99L)).thenReturn(Optional.empty());
   IncidentController controller=new IncidentController(repo,history,"http://localhost:8000");
   assertThrows(ResponseStatusException.class,()->controller.get(99L));
 }
 @Test void createResetsClientSuppliedStatus() {
   IncidentRepository repo=mock(IncidentRepository.class);
   when(repo.save(any())).thenAnswer(inv->inv.getArgument(0));
   IncidentController controller=new IncidentController(repo,mock(InvestigationRepository.class),"http://localhost:8000");
   Incident i=new Incident(); i.title="High latency"; i.serviceName="checkout"; i.severity="HIGH"; i.status="RESOLVED";
   assertEquals("OPEN",controller.create(i).status);
 }
}